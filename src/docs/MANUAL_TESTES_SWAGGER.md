# Manual de testes pelo Swagger

Execute o roteiro na ordem das seções. Use um banco local de testes, pois o roteiro cria contas, altera papéis globais e exclui registros ao final. Use os IDs devolvidos pelas operações. Os números nos exemplos servem para mostrar o formato do JSON.

## 1. Preparar o ambiente

Use Java 21, Maven e Docker com Compose. Execute os comandos na raiz do repositório, onde está o `pom.xml`. Em um ambiente local dedicado, configure as variáveis abaixo no `.env`. Se o arquivo já existir, preserve a configuração e confira se ela aponta para o banco de testes.

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=gestor_swagger
DB_USER=gestor
DB_PASSWORD=senha-local-de-teste
JWT_SECRET_KEY=<chave-Base64>
JWT_EXPIRATION=3600000
ADMIN_EMAIL=admin-swagger@example.com
ADMIN_PASSWORD=SenhaAdmin123!
RAG_ENABLED=false
```

Substitua `<chave-Base64>` pelo resultado de `openssl rand -base64 32`. A conta inicial é criada se o e-mail ainda estiver livre. Se o Compose já tiver um volume inicializado, use as credenciais daquele PostgreSQL ou uma instância dedicada com o banco de testes criado.

```bash
docker compose up -d postgres
mvn spring-boot:run
```

Abra [Swagger UI](http://localhost:8080/swagger-ui/index.html). O [contrato OpenAPI em JSON](http://localhost:8080/v3/api-docs) também é público.

1. Expanda a operação pelo método e pela rota e clique em **Try it out**.
2. Informe os parâmetros de caminho ou de consulta nos campos correspondentes. Quando houver corpo JSON, substitua o exemplo do editor pelo corpo do teste.
3. Clique em **Execute** e confira o código em **Server response**, o corpo e, quando aplicável, o efeito com uma consulta posterior.
4. Para autorizar, copie o JWT em texto puro devolvido pelo cadastro ou login. Clique em **Authorize**, cole apenas o token no campo `bearerAuth` e confirme. Para trocar de conta, use **Authorize → Logout** e autorize com o token da outra conta.

## 2. Autenticação e preparação das contas

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| POST `/api/v1/auth/register` | Cadastre **Gerente**, **Membro** e **Externo**, usando um e-mail diferente para cada conta. Use o corpo abaixo como modelo. | **200**, JWT em texto puro. A conta nasce com `ROLE_MEMBRO`. |
| POST `/api/v1/auth/login` | Envie somente `email` e `password`. Faça login com o administrador inicial e guarde os tokens das quatro contas de teste. | **200**, JWT em texto puro. |
| GET `/api/projetos` | Execute com a autorização removida. | **401**. |

```json
{"username":"Gerente Swagger","email":"gerente-swagger@example.com","password":"SenhaTeste123!"}
```

No cadastro, repita um e-mail já utilizado para conferir **409** e tente um e-mail inválido ou uma senha de menos de seis caracteres para conferir **400**. No login, use a senha errada com um e-mail válido para conferir **401**.

## 3. Usuários, perfis e papéis globais

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| GET `/api/usuarios` | Use **Admin** e anote o `id` de cada conta pelo e-mail. Repita como **Membro**. | **200** com o diretório para **Admin**, **403** para **Membro**. |
| PUT `/api/usuarios/{id}/papeis` | Como **Admin**, use o ID de **Gerente** e envie `{"papeis":["ROLE_MEMBRO"]}`. | **200** com `ROLE_MEMBRO`. A gerência será definida na criação do projeto. |
| GET `/api/usuarios/{id}` | Como **Membro**, consulte o próprio ID e depois o ID de **Externo**. Consulte o ID de **Externo** também como **Admin**. | **200** para o titular e o administrador, **403** para a consulta de perfil alheio pelo membro. Confira que o perfil omite a senha. |
| PUT `/api/usuarios/{id}` | Como **Membro**, envie o corpo de cadastro com seu e-mail, um novo `username` e uma nova `password`. Faça login com a nova senha. | **200**, perfil atualizado com o mesmo papel. A senha anterior passa a retornar **401** no login. |
| POST `/api/usuarios` | Como **Admin**, envie o mesmo formato de cadastro com um e-mail novo, por exemplo `descartavel-swagger@example.com`. Guarde o ID. | **201**, usuário com `ROLE_MEMBRO`. |
| DELETE `/api/usuarios/{id}` | Como **Admin**, exclua o usuário descartável antes de associá-lo a qualquer projeto. Consulte o mesmo ID. | **204** sem corpo e depois **404** na consulta. |

A atribuição de papéis substitui a lista anterior e mantém `ROLE_MEMBRO`. Teste `{"papeis":[]}` como **Admin** para obter **400**. Como **Membro**, tente atribuir `ROLE_ADMINISTRADOR` a si mesmo e confirme **403**.

## 4. Projetos

Use **Gerente**. Crie o projeto principal incluindo **Membro** e **Admin** pelo ID. Mantenha **Externo** fora da equipe para os testes de isolamento. Crie depois um segundo projeto ativo, apenas com o criador, para testar a transferência de tarefas.

```json
{"nome":"Portal de Projetos","descricao":"Projeto para teste do Swagger","equipeIds":[3,1]}
```

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| POST `/api/projetos` | Envie o JSON com os IDs reais. Para o segundo projeto, mude o nome e use `"equipeIds":[]`. | **201**. Guarde os dois IDs. Confira `criadorId`, `equipeIds`, `arquivado: false` e `podeGerenciar: true`. |
| GET `/api/projetos` | Liste como **Gerente**, **Membro**, **Admin** e **Externo**. | **200** para todos. No cenário criado, **Gerente** vê os dois projetos, **Membro** e **Admin** veem o principal e **Externo** tem lista vazia. |
| GET `/api/projetos/{id}` | Consulte o projeto principal como **Membro** e como **Externo**. Consulte o segundo como **Admin**. | **200** para **Membro** no principal, **403** para quem está fora da equipe, inclusive **Admin** no segundo. |
| PUT `/api/projetos/{id}` | Como **Gerente**, envie `{"nome":"Portal de Projetos v2","descricao":"Descrição revisada","equipeIds":null}`. | **200**, nome e descrição atualizados com a equipe preservada. Se enviar uma lista em `equipeIds`, a equipe é substituída e o criador permanece. |
| PATCH `/api/projetos/{id}/arquivar` | Como **Gerente**, arquive o principal sem corpo. Consulte projeto e equipe. Tente editar o projeto. | **200** com `arquivado: true`. Leituras continuam disponíveis e a edição retorna **409**. |
| PATCH `/api/projetos/{id}/desarquivar` | Como **Gerente**, execute sem corpo antes de seguir. | **200** com `arquivado: false`. A edição volta a funcionar. |
| DELETE `/api/projetos/{id}` | Execute somente na limpeza, com **Gerente**. | **204**. A consulta posterior do projeto excluído retorna **404**. |

Repita a criação como **Membro** e como **Admin** para conferir **201**. Cada conta será gerente do projeto que criou. Como **Gerente**, use nome vazio para conferir **400**.

## 5. Equipes

Use o ID do projeto principal em `projetoId`. As mudanças exigem **Gerente** integrante e projeto ativo. A consulta é acessível aos integrantes, inclusive com o projeto arquivado.

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| GET `/api/projetos/{projetoId}/equipe` | Consulte como **Membro** e como **Externo**. | **200** com ID, nome e e-mail dos integrantes em ordem de nome para **Membro**, **403** para **Externo**. |
| PUT `/api/projetos/{projetoId}/equipe` | Como **Gerente**, envie `{"membrosIds":[]}`. Em seguida repita o PUT com os IDs de **Membro** e **Admin**. | **200** nas duas chamadas. A lista vazia mantém o criador. A segunda chamada restaura a equipe de teste. |
| POST `/api/projetos/{projetoId}/equipe/membros` | Envie `{"membrosIds":[4]}` com o ID de **Externo**. Repita a chamada e consulte o projeto com o token de **Externo**. | **200**, integrante incluído uma única vez. O token de **Externo** passa a acessar o projeto. |
| DELETE `/api/projetos/{projetoId}/equipe/membros/{usuarioId}` | Remova **Externo**, consulte o projeto com o token dele e tente remover o criador com o token de **Gerente**. | **204** na remoção, **403** na consulta de **Externo** e **409** na tentativa de remover o criador. |

No POST, envie uma lista com um ID existente e outro inexistente para confirmar **400** e que a equipe permaneceu igual. Tente mudar a equipe como **Membro** para conferir **403**.

## 6. Tarefas e histórico

Como **Gerente**, use o projeto principal ativo. No JSON, substitua `projetoId` pelo ID do projeto e `responsavelId` pelo ID de **Membro**.

```json
{
  "titulo": "Implementar exportação de relatórios",
  "conteudo": "Gerar um CSV para a equipe.",
  "status": "PENDENTE",
  "prioridade": "ALTA",
  "projetoId": 10,
  "requisitos": "Exportar somente dados do projeto.",
  "tecnologias": ["Java", "PostgreSQL"],
  "complexidade": 3,
  "prazo": "2030-06-30",
  "responsavelId": 3
}
```

Os campos obrigatórios são `titulo`, `status`, `prioridade` e `projetoId`. Use `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA` ou `CANCELADA` como status e `BAIXA`, `MEDIA`, `ALTA` ou `URGENTE` como prioridade. Se informado, o responsável precisa integrar o projeto. Use `AAAA-MM-DD` para o prazo e valores de 1 a 5 para a complexidade.

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| POST `/api/tarefas` | Crie a tarefa de exportação e outra chamada “Definir colunas do CSV”, com o mesmo projeto. | **201** em cada criação. Guarde os IDs para os próximos testes. |
| GET `/api/tarefas` | Consulte como **Membro** e como **Externo**. | **200**. **Membro** vê as tarefas do projeto principal e **Externo** recebe uma lista vazia neste cenário. |
| GET `/api/tarefas/{id}` | Consulte a tarefa de exportação como **Membro** e como **Externo**. | **200** para **Membro**, **403** para **Externo**. |
| GET `/api/tarefas/projeto/{projetoId}` | Informe o projeto principal como **Membro**. | **200** com as duas tarefas do projeto. |
| PATCH `/api/tarefas/{id}/status` | Como **Membro**, selecione `novoStatus=EM_ANDAMENTO` no parâmetro de consulta e execute sem corpo. | **200**. O status muda e os demais dados da tarefa são preservados. |
| GET `/api/tarefas/{id}/historico` | Como **Membro**, consulte a tarefa de exportação. | **200**, com eventos como `CRIACAO` e `STATUS`, incluindo ator, status e data. |
| PUT `/api/tarefas/{id}` | Como **Gerente**, reenvie o corpo completo da tarefa de exportação com `"status":"EM_ANDAMENTO"` e um novo conteúdo. Para testar transferência, edite a tarefa de colunas com o ID do segundo projeto e `"responsavelId":null`. | **200**. A exportação é atualizada e a tarefa de colunas só aparece no projeto de destino. **Membro** perde acesso à tarefa transferida. No destino, o histórico mostra apenas eventos desse projeto. Transfira a tarefa de colunas de volta antes dos testes de IA. |
| DELETE `/api/tarefas/{id}` | Como **Gerente**, exclua uma tarefa de teste no projeto ativo durante a limpeza. | **204** e depois **404** ao consultar o mesmo ID. |

Valide também estes casos:

- Tente criar ou editar a tarefa como **Membro** e confirme **403**.
- Envie status inválido, prioridade inválida ou o ID de **Externo** como responsável e confirme **400**.
- Arquive o projeto principal e, como **Membro**, leia a tarefa e tente mudar o status. Confira **200** na leitura e **409** na alteração. Desarquive o projeto para continuar.

## 7. Competências e configuração de domínio

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| PUT `/api/usuarios/{usuarioId}/competencias` | Como **Membro**, use o próprio ID e envie `{"competencias":[{"tecnologia":"Java","tipoAtividade":"Backend","nivel":4}]}`. | **200** com a lista salva. Um novo PUT substitui a lista. Níveis fora de 1 a 5 retornam **400**. |
| GET `/api/usuarios/{usuarioId}/competencias` | Consulte o perfil de **Membro** como titular, como **Admin** e como **Gerente**. | **200** para o titular e **Admin**, **403** para **Gerente**. |
| GET `/api/admin/regras-dominio` | Consulte como **Admin** e guarde a configuração anterior, se precisar restaurá-la. Repita como **Gerente**. | **200** para **Admin**, **403** para **Gerente**. |
| POST `/api/admin/regras-dominio` | Como **Admin**, envie `{"regras":"Use o vocabulário de gestão de projetos.","vocabulario":"Sprint: ciclo de trabalho."}` e repita o GET. | **200**, configuração salva e disponível para as próximas gerações. |

## 8. Consultas de IA, recomendações e feedback

Use o projeto principal e a tarefa de exportação. O modo padrão (`RAG_ENABLED=false`) permite testar a recuperação de dados e o ranking. Nas operações com resposta de IA, confira `resposta`, `fontes`, `alternativa` e `aviso`. Sem o provedor, espere a resposta alternativa (`alternativa: true`), com fontes quando houver dados relevantes. O texto e a ordem das fontes podem variar.

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| POST `/api/ia/consultar` | Como **Membro**, informe `projetoId` no parâmetro de consulta e envie `{"pergunta":"Qual é o status da tarefa Implementar exportação de relatórios?"}`. Repita como **Externo**. | **200** para **Membro**, com fontes do projeto autorizado, e **403** para **Externo**. |
| GET `/api/ia/projetos/{projetoId}/resumo` | Como **Gerente**, execute sem datas. Repita com `inicio=2030-06-01` e `fim=2030-06-30` para filtrar pelo prazo. | **200** com resumo ou dados recuperados do período. Preencha as duas datas ou deixe ambas em branco. Datas invertidas ou um único limite retornam **400**. |
| GET `/api/ia/projetos/{projetoId}/gargalos` | Como **Gerente**, consulte os indicadores. Para exercitar atraso, deixe uma tarefa ativa com prazo anterior à data atual. | **200**, com dados de atraso, sobrecarga e inatividade conforme as tarefas cadastradas. |
| POST `/api/ia/tarefas/{tarefaId}/descricao` | Como **Gerente**, envie `{"pergunta":"Refine a descrição com critérios de aceite para a exportação CSV."}`. | **200** com proposta ou dados recuperados. Confira pelo GET da tarefa que o conteúdo segue igual. |
| GET `/api/ia/tarefas/{tarefaId}/dependencias` | Como **Gerente**, consulte a tarefa de exportação com a tarefa de colunas no mesmo projeto. | **200** com sugestões ou dados recuperados. As sugestões servem para revisão. |
| POST `/api/ia/tarefas/{tarefaId}/recomendacoes` | Como **Gerente**, execute sem corpo. | **201**, com `id` da recomendação, contexto e `ranking` de integrantes. Confira `usuarioId`, `pontuacao` e `justificativa`. |
| GET `/api/ia/tarefas/{tarefaId}/recomendacoes` | Como **Gerente**, liste as recomendações registradas. | **200**, com o registro recém-criado. |
| POST `/api/ia/tarefas/{tarefaId}/recomendacoes/{id}/feedback` | Como **Gerente**, informe o ID da recomendação no campo `id` e envie um dos corpos abaixo. Crie uma recomendação nova para cada decisão. | **200**, com `decisao` e `escolhidoId` conforme o caso. Repetir a decisão para o mesmo registro retorna **409**. |

| Corpo de feedback | O que conferir |
| --- | --- |
| `{"decisao":"ACEITA"}` | O primeiro integrante do ranking vira responsável. Confira `responsavelId` na tarefa e o evento `ATRIBUICAO` no histórico. |
| `{"decisao":"AJUSTADA","usuarioId":3,"observacao":"Escolha revisada pela equipe"}` | O integrante atual informado vira responsável. Substitua 3 pelo ID real. Omitir o ID ou escolher alguém de fora retorna **400**. |
| `{"decisao":"RECUSADA","observacao":"Manter a atribuição atual"}` | A decisão é registrada e o responsável anterior permanece. Informar `usuarioId` na recusa retorna **400**. |

Como **Membro**, tente gerar uma recomendação e consultar o resumo para conferir **403**. Como **Gerente**, arquive o projeto e tente gerar uma recomendação para conferir **409**. Desarquive para continuar.

## 9. Documentos e verificação de fontes

Com a IA externa desabilitada, confirme que a listagem de documentos funciona e que o envio de um arquivo válido retorna **503**. Para completar o fluxo de indexação, ajuste `RAG_ENABLED=true` no mesmo `.env` da preparação e configure `GEMINI_API_KEY` e reinicie a aplicação. O serviço precisa de PostgreSQL com pgvector e permissão para inicializar o índice vetorial. Consulte os logs da aplicação se o provedor ou o índice estiver indisponível. Faça login novamente se os tokens tiverem expirado.

Crie `politica-exportacao.txt` em UTF-8 com este conteúdo de teste:

```text
Política de exportação do Portal de Projetos
Todo CSV de relatórios deve utilizar ponto e vírgula como separador.
```

| Operação | Como testar | Resultado esperado |
| --- | --- | --- |
| POST `/api/admin/projetos/{projetoId}/documentos` | Como **Admin** integrante do projeto principal ativo, abra **Try it out**, selecione o arquivo no campo `arquivo` e execute. Use `multipart/form-data`. | **201**, com `id`, `nome`, `tipo`, `trechos` e `criadoEm`. Guarde o ID do documento. |
| GET `/api/projetos/{projetoId}/documentos` | Liste como **Membro** da equipe e como **Externo**. | **200** com o arquivo indexado para **Membro**, **403** para **Externo**. |
| GET `/api/projetos/{projetoId}/documentos/{documentoId}/trechos` | Como **Membro**, informe os dois IDs. | **200**, com fontes de tipo `DOCUMENTO`, incluindo trecho e identificador. |
| POST `/api/ia/consultar` | Como **Membro**, use o projeto principal e pergunte `{"pergunta":"Segundo a política de exportação, qual separador deve ser utilizado no CSV de relatórios?"}`. | **200**. Confira a menção a ponto e vírgula e compare a fonte com o trecho consultado. |
| DELETE `/api/admin/projetos/{projetoId}/documentos/{documentoId}` | Como **Admin** integrante, exclua o documento depois de verificar a citação. | **204**. O documento some da listagem e a consulta de trechos retorna **404**. |

São aceitos PDF com texto extraível, Markdown e texto UTF-8, até 5 MB. Teste arquivo vazio ou extensão fora da lista para conferir **400**. Teste o envio como **Gerente** sem papel de administrador e como **Admin** no segundo projeto, do qual ele não participa. Ambos devem retornar **403**.

## 10. Limpeza e registro

1. Como **Gerente**, exclua uma das tarefas de teste e confirme **404** na consulta individual.
2. Exclua o projeto principal ainda com uma tarefa de teste e confirme **404** ao consultar o projeto e a tarefa remanescente. Exclua também o segundo projeto. Cada exclusão bem-sucedida retorna **204**.
3. Registre em cada cenário o papel usado, a rota, os parâmetros, o código HTTP e o resultado observado. Em erros, confira `status`, `mensagem` e `erros` quando disponíveis. O código **401** indica falha de autenticação, **403** indica falta de permissão, **404** indica recurso inexistente no escopo solicitado e **409** indica conflito com o estado atual ou com uma restrição de dados.

A suíte complementar e os comandos Maven estão no [guia de testes automatizados](TESTES.md).
