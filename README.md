# gestor-de-projetos-com-retrieval-augmented-generation

[Manual da API e DTOs](src/docs/MANUAL_API.md) · [Roteiro para demonstrar o RAG](src/docs/DEMO_RAG.md)

## Configuração (.env)

| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET_KEY` | Chave HMAC em Base64 (mínimo de 256 bits) |
| `JWT_EXPIRATION` | Validade do token em ms |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Opcional: cria o primeiro administrador na inicialização |

Os papéis `MEMBRO` e `ADMINISTRADOR` são criados automaticamente. Todo usuário cadastrado recebe `MEMBRO`, e o JWT inclui o claim `roles`.

## Projetos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/projetos` | Projetos dos quais o usuário é integrante |
| GET | `/api/projetos/{id}` | Detalhe |
| POST | `/api/projetos` | Envie `{"nome":"Meu projeto"}` para começar com o criador como único integrante e gerente do projeto. Use `equipeIds` para informar a equipe inicial |
| POST | `/api/projetos/{id}/equipe/membros` | O gerente do projeto pode adicionar usuários cadastrados a um projeto ativo enviando `{"membrosIds":[2,3]}` |
| PUT | `/api/projetos/{id}` | Edita nome, descrição e equipe (`equipeIds: null` mantém a equipe). Projetos arquivados retornam 409 |
| PATCH | `/api/projetos/{id}/arquivar` / `desarquivar` | Arquiva ou restaura |
| DELETE | `/api/projetos/{id}` | Exclusão definitiva |

O criador é o gerente inicial. O gerente atual pode transferir a gerência para um integrante por meio de `gerenteId` no `PUT /api/projetos/{id}`. A gerência vale apenas para o projeto correspondente. A resposta inclui `podeAdicionarMembros` para o frontend identificar quem pode usar a inclusão posterior, além de `podeGerenciar` e `meuPapelNoProjeto`.

Erros seguem o formato `{ status, erro, mensagem, erros?, timestamp }`.

## IA e RAG

A API inicia com a IA externa desabilitada. Para habilitar embeddings e respostas geradas, use PostgreSQL com pgvector (imagem incluída no Compose) e defina `RAG_ENABLED=true` e `GEMINI_API_KEY` no ambiente da aplicação. Os modelos podem ser configurados por `GEMINI_CHAT_MODEL` e `GEMINI_EMBEDDING_MODEL`. O tempo limite do Gemini pode ser ajustado por `GEMINI_TIMEOUT_SECONDS` (padrão de 30 segundos). Na inicialização, o usuário do banco precisa ter acesso à extensão `vector` e permissão para criar a tabela de vetores e o índice HNSW. Preserve a dimensão do índice ao mudar o modelo ou reconstrua a indexação com a nova dimensão.

| Recurso | Operação |
| --- | --- |
| Competências | `GET/PUT /api/usuarios/{id}/competencias` |
| Indexar documento | `POST /api/admin/projetos/{id}/documentos` com o campo multipart `arquivo` |
| Consultar fontes | `GET /api/projetos/{id}/documentos` e `GET /api/projetos/{id}/documentos/{docId}/trechos` |
| Configurar domínio | `GET/POST /api/admin/regras-dominio` |
| Consulta em linguagem natural | `POST /api/ia/consultar?projetoId={id}` com `{"pergunta":"Quais tarefas estão em andamento?"}` |
| Resumo e gargalos | `GET /api/ia/projetos/{id}/resumo` e `GET /api/ia/projetos/{id}/gargalos` |
| Descrição e dependências | `POST /api/ia/tarefas/{id}/descricao` com `{"pergunta":"Refine a descrição"}` e `GET /api/ia/tarefas/{id}/dependencias` |
| Sugestão de responsável | `POST /api/ia/tarefas/{id}/recomendacoes` após salvar a tarefa. Use `GET` na mesma rota para consultar os registros |
| Decisão | `POST /api/ia/tarefas/{id}/recomendacoes/{recomendacaoId}/feedback` |
| Histórico | `GET /api/tarefas/{id}/historico` |

O resumo aceita `inicio` e `fim` em formato ISO para filtrar pelo prazo das tarefas. A recomendação considera competências, conclusões registradas, desempenho e carga dentro do projeto. Ao receber `{"decisao":"ACEITA"}`, a API atribui o primeiro integrante do ranking por padrão. `AJUSTADA` exige `usuarioId` de um integrante atual. `RECUSADA` registra a decisão. A descrição e as dependências são sugestões para revisão.

Os documentos aceitos são PDF com texto extraível, Markdown e texto UTF-8 de até 5 MB. Com o Gemini indisponível, a API de consulta devolve os dados recuperados com citações e a recomendação mantém a pontuação calculada no servidor. A ingestão depende dos embeddings e retorna 503 quando o provedor está indisponível. As permissões e os exemplos estão no Swagger em `/swagger-ui/index.html`.
