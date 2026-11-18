# Controle de acesso

Base: RF01, RNF02, RNF03 e seção 4.1 de `APS_Entrega_01_Requisitos_e_Prototipos.docx`; seções 2.1, 3 e 6 do `der.pdf`.

O DER adota papéis globais e deixa papéis por projeto como decisão futura. A implementação usa `TipoRole`, preservando os nomes sem prefixo na tabela de papéis e gerando authorities com exatamente um `ROLE_`. Nenhuma migração de esquema é necessária.

| Operação | Membro | Gerente | Administrador |
| --- | --- | --- | --- |
| Consultar projetos e tarefas da própria equipe | Sim | Sim | Sim |
| Atualizar status de tarefa da própria equipe | Sim | Sim | Sim |
| Criar projetos | Não | Sim | Somente se também for gerente |
| Editar, arquivar, excluir projeto; organizar equipe e tarefas | Não | Apenas na própria equipe | Somente se também for gerente e integrante |
| Consultar diretório de usuários para formar equipe | Não | Sim | Sim |
| Consultar/editar perfil próprio | Sim | Sim | Sim |
| Gerenciar outras contas e atribuir papéis globais | Não | Não | Sim |
| Acessar rotas `/api/admin/**` | Não | Não | Sim |

A associação à equipe é obrigatória para visualizar qualquer projeto, inclusive para administradores. O criador é incluído na equipe ao criar o projeto. A autoria não preserva permissões de gerente depois da retirada desse papel global. Gerentes executam os casos de uso de membro, mesmo quando possuem somente `GERENTE` no banco.

As decisões de acesso estão nos serviços (`AccessPolicy`), além das restrições por rota no Spring Security. DAOs e repositórios permanecem componentes internos de persistência. As listagens de projetos e tarefas filtram a equipe na consulta ao banco. Uma transferência de tarefa exige autorização nos dois projetos. Projetos arquivados permitem leitura, mas bloqueiam alterações em suas tarefas.

## Endpoints adicionados

- `PATCH /api/tarefas/{id}/status?novoStatus=EM_ANDAMENTO`: atualiza apenas o status, usando `StatusTarefa`.
- `PUT /api/usuarios/{id}/papeis`: recebe `{"papeis":["ROLE_GERENTE"]}`; somente administrador. O papel mínimo `ROLE_MEMBRO` é preservado. A resposta de usuário inclui `papeis`.

O cadastro público continua atribuindo somente membro. O administrador inicial pode ser criado com `ADMIN_EMAIL` e `ADMIN_PASSWORD`, como já previsto no projeto. Para organizar projetos, ele pode atribuir a si também o papel de gerente pelo endpoint administrativo. Os papéis atuais são carregados do banco durante a autenticação e autorização, sem confiar em permissões antigas do JWT.

Sem autenticação, as rotas protegidas retornam 401; sem permissão, 403. Senhas criadas ou alteradas pelo serviço de usuários são armazenadas com BCrypt.

## Limites e testes

Os testes em `src/test/java/.../security` cobrem isolamento entre equipes, matriz de papéis, bloqueio de promoção indevida, atualização de status, transferências e restrições HTTP. São independentes de Docker e de serviços de IA.

Nesta revisão do código ainda não existem controladores/serviços de IA, documentos ou regras de domínio. O filtro reserva `/api/admin/**` ao administrador, mas a filtragem de fontes RAG por projeto deverá usar a mesma política quando esses recursos forem implementados.

Os testes antigos em `controller` foram adaptados aos papéis globais, às rotas existentes e ao formato atual dos retornos. A suíte completa e seus cenários estão descritos em [TESTES.md](TESTES.md).

## Swagger / OpenAPI

Com a aplicação em execução, acesse `/swagger-ui/index.html` para explorar os cinco controllers ou `/v3/api-docs` para obter o contrato OpenAPI em JSON. A documentação é pública.

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

Para formar a equipe, consulte os usuários em `GET /api/usuarios` e envie seus IDs no `PUT`:

```json
{"membrosIds": [2, 3]}
```

O mesmo corpo serve para adicionar integrantes pelo `POST`. Os usuários precisam estar cadastrados. IDs inexistentes retornam 400, sem inclusão parcial; IDs nulos, zero ou negativos também são rejeitados. IDs repetidos ou já presentes não duplicam integrantes.

O `PUT` retorna 200 com a equipe resultante e sempre preserva o criador, mesmo quando `membrosIds` é uma lista vazia. O `POST` também retorna 200 com a equipe resultante. O `DELETE` retorna 204; tentar remover o criador retorna 409, e remover alguém que não pertence à equipe retorna 404. Os retornos contêm apenas ID, nome e e-mail dos integrantes.

Participar da equipe concede acesso ao projeto e às suas tarefas; a remoção revoga esse acesso, sem excluir a conta ou alterar papéis globais. Um gerente que não seja o criador pode remover a si mesmo e perderá acesso ao projeto. A equipe também pode continuar sendo informada por `equipeIds` na criação/edição do projeto.

Projetos arquivados permitem consultar a equipe, mas alterações retornam 409. Administradores precisam também do papel de gerente e de participação na equipe para alterá-la. Os testes `EquipeControllerIntegrationTest` verificam os endpoints com Spring Security e persistência H2, incluindo a revogação de acesso a projetos e tarefas.
