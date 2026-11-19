package com.mendes15.gestor_de_projetos_com_retrieval_augmented_generation.rag;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class DocumentParser {
    private final RagProperties properties;
    public record Chunk(int ordem, Integer pagina, String texto) {}

    public List<Chunk> parse(String name, byte[] bytes) {
        if (bytes.length==0 || bytes.length>5*1024*1024) throw bad("Arquivo vazio ou maior que 5 MB.");
        String lower=name.toLowerCase(Locale.ROOT);
        List<Chunk> result=new ArrayList<>();
        try {
            if (lower.endsWith(".pdf")) {
                try (var pdf=Loader.loadPDF(bytes)) {
                    if (pdf.getNumberOfPages()>200) throw bad("PDF deve ter até 200 páginas.");
                    if (!pdf.getCurrentAccessPermission().canExtractContent()) throw bad("PDF não permite extração de texto.");
                    var stripper=new PDFTextStripper();
                    int count=0;
                    for (int page=1; page<=pdf.getNumberOfPages();page++) {
                        stripper.setStartPage(page); stripper.setEndPage(page);
                        String text=stripper.getText(pdf); count+=text.length(); checkLength(count);
                        split(text,page,result);
                    }
                }
            } else if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
                String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
                if (text.indexOf(0)>=0) throw bad("Arquivo de texto contém dados binários.");
                checkLength(text.length()); split(text,null,result);
            } else throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Use PDF, Markdown ou texto UTF-8.");
        } catch (java.io.IOException ex) { throw bad("Não foi possível extrair o texto do arquivo."); }
        if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Arquivo sem texto extraível. PDFs digitalizados precisam de OCR antes do envio.");
        return result;
    }

    private void checkLength(int length) {
        if (length>properties.getMaxTextChars()) throw bad("Texto excede o limite de "+properties.getMaxTextChars()+" caracteres.");
    }
    private void split(String text,Integer page,List<Chunk> out) {
        text=text.replace("\r\n","\n").replace("\u0000","").strip();
        int size=properties.getChunkSize(), overlap=properties.getOverlap();
        if (size<100 || overlap<0 || overlap>=size) throw new IllegalStateException("Configuração de chunk/overlap inválida.");
        for (int start=0;start<text.length();) {
            int end=Math.min(start+size,text.length());
            String chunk=text.substring(start,end).strip();
            if (!chunk.isBlank()) out.add(new Chunk(out.size(),page,chunk));
            if (end==text.length()) break;
            start=end-overlap;
        }
        if (out.size()>100) throw bad("Documento excede 100 trechos; divida o arquivo.");
    }
    private ResponseStatusException bad(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,text); }
}
