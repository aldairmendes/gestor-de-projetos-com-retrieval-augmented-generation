# Demonstração do RAG no Swagger

O cenário usa um projeto fictício, **Aurora | Demonstração RAG**, e um [procedimento de implantação](demo-aurora.md). O código de aprovação e a janela de manutenção estão no documento, que a API divide em trechos, transforma em embeddings e indexa no PostgreSQL com pgvector. Ao receber a pergunta, a API recupera trechos do projeto e os envia ao Gemini para gerar a resposta com citações.

## Preparar o ambiente

1. Configure o banco, o JWT e o administrador inicial no `.env`, seguindo o [README](../../README.md). Defina `RAG_ENABLED=true` e `GEMINI_API_KEY` com a chave da sua conta. Use `GEMINI_CHAT_MODEL` para selecionar um modelo de geração disponível para a chave. No ambiente local validado foi usado `gemini-2.5-flash-lite`.
2. Na raiz do projeto, inicie o banco e a API com Java 21.

   ```bash
   docker compose up -d postgres
   mvn spring-boot:run
   ```

3. Abra `http://localhost:8080/swagger-ui/index.html`. Execute `POST /api/v1/auth/login` com as credenciais do administrador e cole o JWT em **Authorize**. Faça essa etapa antes de gravar para manter senha e JWT fora do vídeo.

## Gravar o fluxo

1. Em `POST /api/projetos`, crie um projeto com `{"nome":"Aurora | Demonstração RAG"}`. O retorno 201 inclui o `id`; o administrador que criou o projeto já integra a equipe. Para mostrar apenas a consulta, use o projeto existente retornado por `GET /api/projetos`.
2. Em `POST /api/admin/projetos/{projetoId}/documentos`, informe esse ID e envie [demo-aurora.md](demo-aurora.md) no campo `arquivo`. Mostre o 201 e o campo `trechos`. Para mostrar o texto indexado, use `GET /api/projetos/{projetoId}/documentos/{documentoId}/trechos` com o ID retornado pelo upload.
3. Em `POST /api/ia/consultar`, informe `projetoId` na query string e envie o corpo abaixo.

   ```json
   {
     "pergunta": "Qual é o código de aprovação da implantação do Projeto Aurora e qual é a janela de manutenção?"
   }
   ```

4. Mostre na resposta o código **AURORA-742**, a janela **terça-feira, das 22h às 23h, no horário de Fortaleza** e o item de `fontes` com `tipo: "DOCUMENTO"`, o título `demo-aurora.md` e o trecho citado. O ID da citação varia conforme o banco. `alternativa: false` confirma que houve síntese do Gemini.

Se o upload devolver 503, confira o índice pgvector e o acesso ao modelo de embeddings. Se a consulta retornar `alternativa: true`, a API exibiu os dados recuperados em modo alternativo. Confira os logs para identificar falhas de comunicação ou o status HTTP retornado pelo Gemini. O tempo limite por chamada é configurável por `GEMINI_TIMEOUT_SECONDS`, com padrão de 30 segundos.

O [manual da API](MANUAL_API.md) contém as demais operações e os campos dos DTOs.
