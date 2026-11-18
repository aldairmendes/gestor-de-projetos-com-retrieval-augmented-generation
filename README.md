# gestor-de-projetos-com-retrieval-augmented-generation

## Configuração (.env)

| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET_KEY` | Chave HMAC em Base64 (mínimo de 256 bits) |
| `JWT_EXPIRATION` | Validade do token em ms |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Opcional: cria o primeiro administrador na inicialização |

Os papéis `MEMBRO`, `GERENTE` e `ADMINISTRADOR` são criados automaticamente. Todo usuário cadastrado recebe `MEMBRO`, e o JWT inclui o claim `roles`.

## Projetos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/projetos` | Projetos visíveis ao usuário (ADMINISTRADOR vê todos; os demais, os que criaram ou de cuja equipe fazem parte) |
| GET | `/api/projetos/{id}` | Detalhe |
| POST | `/api/projetos` | `{ nome, descricao?, equipeIds? }`. O criador vira gerente e sempre integra a equipe |
| PUT | `/api/projetos/{id}` | Edita nome, descrição e equipe (`equipeIds: null` mantém a equipe). Projetos arquivados retornam 409 |
| PATCH | `/api/projetos/{id}/arquivar` / `desarquivar` | Arquiva ou restaura |
| DELETE | `/api/projetos/{id}` | Exclusão definitiva |

Editar, arquivar, definir a equipe e excluir são permitidos ao criador, a um `GERENTE` que esteja na equipe ou a um `ADMINISTRADOR`. A resposta traz `podeGerenciar` e `meuPapelNoProjeto` para o frontend adaptar a interface.

Erros seguem o formato `{ status, erro, mensagem, erros?, timestamp }`.
