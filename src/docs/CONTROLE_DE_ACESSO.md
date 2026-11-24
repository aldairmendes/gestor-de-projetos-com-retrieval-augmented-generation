# Controle de acesso

Base: RF01, RNF02, RNF03 e seção 4.1 de `APS_Entrega_01_Requisitos_e_Prototipos.docx`; seções 2.1, 3 e 6 do `der.pdf`.

Os papéis globais são `ROLE_MEMBRO` e `ROLE_ADMINISTRADOR`. Cada projeto registra seu gerente em `projetos.gerente_id`, com o criador como gerente inicial.

| Operação | Acesso |
| --- | --- |
| Criar projeto, com equipe inicial opcional | Usuário autenticado |
| Consultar projeto e suas tarefas ou atualizar status de tarefa | Integrante da equipe |
| Editar, arquivar ou excluir projeto, organizar equipe, quadros e tarefas | Gerente do projeto, integrante da equipe |
| Consultar diretório de usuários para formar equipe | Gerente integrante de algum projeto ou administrador |
| Consultar ou editar perfil | Titular ou administrador |
| Gerenciar contas, atribuir papéis globais e acessar `/api/admin/**` | Administrador |

A resposta do projeto inclui `gerenteId`, `meuPapelNoProjeto`, `podeGerenciar` e `podeAdicionarMembros`. Em um projeto ativo, o gerente pode transferir a gerência indicando o `gerenteId` de um integrante no `PUT /api/projetos/{id}`. O criador permanece na equipe após a transferência.

Na inicialização, projetos legados com criador recebem esse usuário como gerente. A migração remove as atribuições globais legadas de gerente e garante o papel de membro às contas afetadas.

As decisões de acesso estão nos serviços (`AccessPolicy`), além das restrições por rota no Spring Security. DAOs e repositórios permanecem componentes internos de persistência. As listagens de projetos e tarefas filtram a equipe na consulta ao banco. Uma transferência de tarefa exige autorização nos dois projetos. Projetos arquivados permitem leitura, mas bloqueiam alterações em suas tarefas.

## Endpoints adicionados

- `PATCH /api/tarefas/{id}/status?novoStatus=EM_ANDAMENTO`: atualiza apenas o status, usando `StatusTarefa`.
- `PUT /api/usuarios/{id}/papeis`: recebe `{"papeis":["ROLE_ADMINISTRADOR"]}`; somente administrador. O papel mínimo `ROLE_MEMBRO` é preservado. A resposta de usuário inclui `papeis`.

O cadastro público continua atribuindo somente membro. O administrador inicial pode ser criado com `ADMIN_EMAIL` e `ADMIN_PASSWORD`, como já previsto no projeto. Os papéis atuais são carregados do banco durante a autenticação e autorização, sem confiar em permissões antigas do JWT.

Sem autenticação, as rotas protegidas retornam 401; sem permissão, 403. Senhas criadas ou alteradas pelo serviço de usuários são armazenadas com BCrypt.

## Limites e testes

Os testes em `src/test/java/.../security` cobrem isolamento entre equipes, matriz de papéis, bloqueio de promoção indevida, atualização de status, transferências e restrições HTTP. São independentes de Docker e de serviços de IA.

| Operação de IA | Acesso |
| --- | --- |
| Consultar o assistente, documentos e trechos | Integrante do projeto |
| Gerar descrição, sugerir dependências, ver resumos e gargalos, consultar e gerar recomendações, registrar decisões | Gerente integrante do projeto |
| Cadastrar competências | Titular do perfil ou administrador |
| Indexar ou excluir documentos | Administrador integrante de projeto ativo |
| Consultar ou alterar regras e vocabulário de domínio | Administrador |

O histórico da tarefa e as recomendações ficam vinculados ao projeto em que foram registrados. Apenas o histórico do projeto atual é exibido após uma transferência.

Os testes antigos em `controller` foram adaptados aos papéis globais, às rotas existentes e ao formato atual dos retornos. A suíte completa e seus cenários estão descritos em [TESTES.md](TESTES.md).

## Swagger / OpenAPI

Com a aplicação em execução, acesse `/swagger-ui/index.html` para explorar as operações da API ou `/v3/api-docs` para obter o contrato OpenAPI em JSON. A documentação é pública.

1. Execute `POST /api/v1/auth/login` com e-mail e senha (ou use o cadastro público).
2. Copie o JWT retornado como texto puro.
3. Clique em **Authorize** e informe somente o token, sem o prefixo `Bearer`.
4. Execute as operações protegidas. O token não dispensa o papel exigido nem a participação na equipe.

Cada operação documenta suas permissões, parâmetros, exemplos de corpo e respostas HTTP. Cadastro e login são públicos; projetos, tarefas e usuários exigem JWT. Os schemas de requisição e resposta são derivados dos DTOs e de suas validações.

## Equipes dos projetos

A equipe é o conjunto de usuários vinculados a um projeto (`projeto_usuarios`). Cada projeto tem sua própria equipe; o criador já participa desde a criação. É possível formá-la e alterá-la sem editar os demais dados do projeto.

| Método e rota | Finalidade | Acesso |
| --- | --- | --- |
| `GET /api/projetos/{projetoId}/equipe` | Consultar integrantes, ordenados por nome | Integrante da equipe |
| `PUT /api/projetos/{projetoId}/equipe` | Definir/substituir os integrantes | Gerente integrante; projeto ativo |
| `POST /api/projetos/{projetoId}/equipe/membros` | Adicionar integrantes preservando os atuais | Gerente integrante; projeto ativo |
| `DELETE /api/projetos/{projetoId}/equipe/membros/{usuarioId}` | Remover um integrante | Gerente integrante; projeto ativo |

Para incluir pessoas após a criação, envie seus IDs no `POST`. Gerentes e administradores podem consultar os IDs em `GET /api/usuarios`:

```json
{"membrosIds": [2, 3]}
```

Gerentes da equipe podem usar o mesmo corpo no `PUT` para substituir a equipe. Os usuários precisam estar cadastrados. IDs inexistentes retornam 400, sem inclusão parcial; IDs nulos, zero ou negativos também são rejeitados. IDs repetidos ou já presentes não duplicam integrantes.

O `PUT` retorna 200 com a equipe resultante e sempre preserva o criador e o gerente atual, mesmo quando `membrosIds` é uma lista vazia. O `POST` também retorna 200 com a equipe resultante. O `DELETE` retorna 204; tentar remover o criador ou o gerente atual retorna 409, e remover alguém que não pertence à equipe retorna 404. Os retornos contêm apenas ID, nome e e-mail dos integrantes.

Participar da equipe concede acesso ao projeto e às suas tarefas; a remoção revoga esse acesso, sem excluir a conta ou alterar papéis globais. Para remover o gerente atual, transfira a gerência primeiro. A equipe também pode continuar sendo informada por `equipeIds` na criação/edição do projeto.

Projetos arquivados permitem consultar a equipe, mas alterações retornam 409. Para substituir a equipe ou remover integrantes, administradores também precisam ser o gerente do projeto e integrar a equipe. Os testes `EquipeControllerIntegrationTest` verificam os endpoints com Spring Security e persistência H2, incluindo a revogação de acesso a projetos e tarefas.
