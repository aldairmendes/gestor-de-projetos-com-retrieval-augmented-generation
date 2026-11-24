# Manual da API

Base local `http://localhost:8080`. O Swagger está em `/swagger-ui/index.html` e o contrato OpenAPI em `/v3/api-docs`.

Envie JSON com `Content-Type: application/json`. No upload, use `multipart/form-data` com a parte `arquivo`. Login e cadastro retornam o JWT como texto puro. Nas demais operações, envie `Authorization: Bearer <JWT>`. No Swagger, cole somente o JWT em **Authorize**.

Os IDs são inteiros de 64 bits. Em rotas, preencha os valores entre chaves. Datas sem hora usam `AAAA-MM-DD`. Instantes usam ISO 8601, por exemplo `2026-11-23T22:00:00Z`.

Consulte as [permissões por papel e equipe](CONTROLE_DE_ACESSO.md) e o [roteiro de demonstração](DEMO_RAG.md).

## Operações

### Administração da IA

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `POST /api/admin/projetos/{projetoId}/documentos` | Indexar documento | — | `arquivo` (multipart) | 201 [Documento](#documento) |
| `DELETE /api/admin/projetos/{projetoId}/documentos/{documentoId}` | Excluir documento indexado | — | — | 204 Sem corpo |
| `GET /api/admin/regras-dominio` | Consultar configuração de domínio | — | — | 200 [Dominio](#dominio) |
| `POST /api/admin/regras-dominio` | Salvar configuração de domínio | — | [Dominio](#dominio) | 200 [Dominio](#dominio) |

### Autenticação

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `POST /api/v1/auth/login` | Autenticar usuário | — | [UsuarioLoginRequestDTO](#usuariologinrequestdto) | 200 Texto |
| `POST /api/v1/auth/register` | Cadastrar usuário | — | [UsuarioRegisterRequestDTO](#usuarioregisterrequestdto) | 200 Texto |

### Base de conhecimento

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/projetos/{projetoId}/documentos` | Listar documentos do projeto | — | — | 200 Lista de [Documento](#documento) |
| `GET /api/projetos/{projetoId}/documentos/{documentoId}/trechos` | Consultar trechos de um documento | — | — | 200 Lista de [Fonte](#fonte) |
| `GET /api/usuarios/{usuarioId}/competencias` | Consultar competências | — | — | 200 Lista de [CompetenciaRequest](#competenciarequest) |
| `PUT /api/usuarios/{usuarioId}/competencias` | Atualizar competências | — | [Competencias](#competencias) | 200 Lista de [CompetenciaRequest](#competenciarequest) |

### Equipes

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/projetos/{projetoId}/equipe` | Consultar equipe do projeto | — | — | 200 Lista de [TeamMemberDTO](#teammemberdto) |
| `PUT /api/projetos/{projetoId}/equipe` | Definir equipe do projeto | — | [EquipeRequest](#equiperequest) | 200 Lista de [TeamMemberDTO](#teammemberdto) |
| `POST /api/projetos/{projetoId}/equipe/membros` | Adicionar membros à equipe | — | [EquipeRequest](#equiperequest) | 200 Lista de [TeamMemberDTO](#teammemberdto) |
| `DELETE /api/projetos/{projetoId}/equipe/membros/{usuarioId}` | Remover membro da equipe | — | — | 204 Sem corpo |

### Inteligência artificial

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `POST /api/ia/consultar` | Consultar fontes do projeto | `projetoId` | [Consulta](#consulta) | 200 [Resposta](#resposta) |
| `GET /api/ia/projetos/{projetoId}/gargalos` | Identificar gargalos | — | — | 200 [Resposta](#resposta) |
| `GET /api/ia/projetos/{projetoId}/resumo` | Resumir andamento | `inicio` (opcional), `fim` (opcional) | — | 200 [Resposta](#resposta) |
| `GET /api/ia/tarefas/{tarefaId}/dependencias` | Sugerir dependências | — | — | 200 [Resposta](#resposta) |
| `POST /api/ia/tarefas/{tarefaId}/descricao` | Gerar ou refinar descrição | — | [Consulta](#consulta) | 200 [Resposta](#resposta) |
| `GET /api/ia/tarefas/{tarefaId}/recomendacoes` | Listar recomendações registradas | — | — | 200 Lista de [Recomendacao](#recomendacao) |
| `POST /api/ia/tarefas/{tarefaId}/recomendacoes` | Recomendar responsáveis | — | — | 201 [Recomendacao](#recomendacao) |
| `POST /api/ia/tarefas/{tarefaId}/recomendacoes/{id}/feedback` | Registrar decisão sobre recomendação | — | [Feedback](#feedback) | 200 [Recomendacao](#recomendacao) |

### Projetos

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/projetos` | Listar meus projetos | — | — | 200 Lista de [ProjetoResponseDTO](#projetoresponsedto) |
| `POST /api/projetos` | Criar projeto | — | [ProjetoRequestDTO](#projetorequestdto) | 201 [ProjetoResponseDTO](#projetoresponsedto) |
| `GET /api/projetos/{id}` | Consultar projeto | — | — | 200 [ProjetoResponseDTO](#projetoresponsedto) |
| `PUT /api/projetos/{id}` | Atualizar projeto e equipe | — | [ProjetoRequestDTO](#projetorequestdto) | 200 [ProjetoResponseDTO](#projetoresponsedto) |
| `DELETE /api/projetos/{id}` | Excluir projeto | — | — | 204 Sem corpo |
| `PATCH /api/projetos/{id}/arquivar` | Arquivar projeto | — | — | 200 [ProjetoResponseDTO](#projetoresponsedto) |
| `PATCH /api/projetos/{id}/desarquivar` | Desarquivar projeto | — | — | 200 [ProjetoResponseDTO](#projetoresponsedto) |

### Quadros

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/projetos/{projetoId}/quadros` | Listar quadros do projeto | — | — | 200 Lista de [QuadroResposta](#quadroresposta) |
| `POST /api/projetos/{projetoId}/quadros` | Criar quadro com colunas na ordem informada | — | [Configuracao](#configuracao) | 201 [QuadroResposta](#quadroresposta) |
| `GET /api/projetos/{projetoId}/quadros/{quadroId}` | Consultar configuração do quadro | — | — | 200 [QuadroResposta](#quadroresposta) |
| `PUT /api/projetos/{projetoId}/quadros/{quadroId}` | Configurar quadro e colunas | — | [Configuracao](#configuracao) | 200 [QuadroResposta](#quadroresposta) |

### Tarefas

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/tarefas` | Listar minhas tarefas | — | — | 200 Lista de [TarefaResponseDTO](#tarefaresponsedto) |
| `POST /api/tarefas` | Criar tarefa | — | [TarefaRequestDTO](#tarefarequestdto) | 201 [TarefaResponseDTO](#tarefaresponsedto) |
| `GET /api/tarefas/projeto/{projetoId}` | Listar tarefas de um projeto | — | — | 200 Lista de [TarefaResponseDTO](#tarefaresponsedto) |
| `GET /api/tarefas/{id}` | Consultar tarefa | — | — | 200 [TarefaResponseDTO](#tarefaresponsedto) |
| `PUT /api/tarefas/{id}` | Atualizar ou transferir tarefa | — | [TarefaRequestDTO](#tarefarequestdto) | 200 [TarefaResponseDTO](#tarefaresponsedto) |
| `DELETE /api/tarefas/{id}` | Excluir tarefa | — | — | 204 Sem corpo |
| `GET /api/tarefas/{id}/historico` | Consultar histórico da tarefa | — | — | 200 Lista de [Historico](#historico) |
| `PATCH /api/tarefas/{id}/status` | Atualizar status da tarefa | `novoStatus` | — | 200 [TarefaResponseDTO](#tarefaresponsedto) |

### Usuários

| Rota | Operação | Query string | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| `GET /api/usuarios` | Consultar diretório de usuários | — | — | 200 Lista de [UsuarioResponseDTO](#usuarioresponsedto) |
| `POST /api/usuarios` | Criar usuário como administrador | — | [UsuarioRegisterRequestDTO](#usuarioregisterrequestdto) | 201 [UsuarioResponseDTO](#usuarioresponsedto) |
| `GET /api/usuarios/{id}` | Consultar perfil | — | — | 200 [UsuarioResponseDTO](#usuarioresponsedto) |
| `PUT /api/usuarios/{id}` | Atualizar perfil | — | [UsuarioRegisterRequestDTO](#usuarioregisterrequestdto) | 200 [UsuarioResponseDTO](#usuarioresponsedto) |
| `DELETE /api/usuarios/{id}` | Excluir usuário | — | — | 204 Sem corpo |
| `PUT /api/usuarios/{id}/papeis` | Substituir papéis globais | — | [AlterarPapeisRequest](#alterarpapeisrequest) | 200 [UsuarioResponseDTO](#usuarioresponsedto) |

`novoStatus` aceita os valores de status de [TarefaRequestDTO](#tarefarequestdto). `inicio` e `fim` são datas para filtrar tarefas pelo prazo. `projetoId` é obrigatório em `POST /api/ia/consultar`.

As falhas usam [ErroApi](#erroapi). As mais comuns são 400 (dados inválidos), 401 (autenticação), 403 (permissão), 404 (recurso não encontrado), 409 (conflito), 413 (upload grande demais), 415 (formato não suportado), 422 (arquivo sem texto extraível) e 503 (provedor ou índice indisponível na ingestão). Uma consulta em modo extrativo pode retornar 200 com `alternativa: true`.

## DTOs

Cada tabela lista os campos JSON. A coluna **Obrigatório** se aplica ao envio do JSON. Campos de texto obrigatórios rejeitam valores em branco, com exceção de `Dominio.vocabulario`, que aceita texto vazio.

### AlterarPapeisRequest

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `papeis` | Lista de textos | Sim | Pelo menos um papel. Por item (`ROLE_MEMBRO` ou `ROLE_ADMINISTRADOR`). |

### Candidato

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `usuarioId` | Inteiro | — | — |
| `pontuacao` | Número | — | — |
| `justificativa` | Texto | — | — |
| `fontes` | Lista de textos | — | — |

### ColunaConfiguracao

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | Não | ID positivo. Informe o ID para editar uma coluna existente. Omita para adicionar uma coluna. |
| `nome` | Texto | Sim | Máximo de 120 caracteres. |

### ColunaResposta

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `nome` | Texto | — | — |
| `posicao` | Inteiro | — | — |

### CompetenciaRequest

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `tecnologia` | Texto | Sim | Máximo de 100 caracteres. |
| `tipoAtividade` | Texto | Sim | Máximo de 100 caracteres. |
| `nivel` | Inteiro | Sim | Mínimo de 1. Máximo de 5. |

### Competencias

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `competencias` | Lista de [CompetenciaRequest](#competenciarequest) | Sim | Máximo de 50 itens. |

### Configuracao

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | Texto | Sim | Máximo de 120 caracteres. |
| `colunas` | Lista de [ColunaConfiguracao](#colunaconfiguracao) | Sim | Pelo menos uma coluna, na ordem desejada. |

### Consulta

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `pergunta` | Texto | Sim | Máximo de 4000 caracteres. |

### Documento

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `nome` | Texto | — | — |
| `tipo` | Texto | — | — |
| `trechos` | Inteiro | — | — |
| `criadoEm` | Instante ISO 8601 | — | — |

### Dominio

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `regras` | Texto | Sim | Máximo de 4000 caracteres. |
| `vocabulario` | Texto | Sim | Máximo de 4000 caracteres. |

### EquipeRequest

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `membrosIds` | Lista de inteiros | Sim | IDs positivos. IDs repetidos são ignorados. |

### ErroApi

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `status` | Inteiro | — | — |
| `erro` | Texto | — | — |
| `mensagem` | Texto | — | — |
| `timestamp` | Instante ISO 8601 | — | — |
| `erros` | Objeto de mensagens de texto por campo | — | Presente quando há detalhes de validação por campo. |

### Feedback

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `decisao` | Texto | Sim | `ACEITA`, `AJUSTADA`, `RECUSADA`. |
| `usuarioId` | Inteiro | Condicional | ID positivo. Obrigatório quando `decisao` é `AJUSTADA`. |
| `observacao` | Texto | Não | Máximo de 2000 caracteres. |

### Fonte

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Texto | — | Identificador citado no texto. Prefixos `D` para trecho de documento, `P` para projeto e `T` para tarefa, entre outros. |
| `tipo` | Texto | — | — |
| `recursoId` | Inteiro | — | ID do recurso original. Para `DOCUMENTO`, identifica o documento. |
| `titulo` | Texto | — | — |
| `pagina` | Inteiro | — | Página do PDF, começando em 1. Pode ser nulo. |
| `trecho` | Texto | — | — |
| `similaridade` | Número | — | — |

### Historico

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `acao` | Texto | — | — |
| `atorId` | Inteiro | — | — |
| `responsavelId` | Inteiro | — | — |
| `status` | Texto | — | — |
| `criadoEm` | Instante ISO 8601 | — | — |

### ProjetoRequestDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `nome` | Texto | Sim | Máximo de 255 caracteres. |
| `descricao` | Texto | Não | — |
| `equipeIds` | Lista de inteiros | Não | Na edição, ausência ou nulo mantém a equipe. Lista vazia preserva o criador e o gerente atual, salvo transferência de gerência. |
| `gerenteId` | Inteiro | Não | Na edição, transfere a gerência para um integrante da equipe resultante. |

### ProjetoResponseDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `nome` | Texto | — | — |
| `descricao` | Texto | — | — |
| `arquivado` | Booleano | — | — |
| `criadorId` | Inteiro | — | — |
| `equipe` | Lista de [TeamMemberDTO](#teammemberdto) | — | — |
| `gerenteId` | Inteiro | — | ID do gerente atual. |
| `equipeIds` | Lista de inteiros | — | — |
| `tarefasIds` | Lista de inteiros | — | — |
| `createdAt` | Instante ISO 8601 | — | — |
| `meuPapelNoProjeto` | Texto | — | — |
| `podeGerenciar` | Booleano | — | — |
| `podeAdicionarMembros` | Booleano | — | — |

### QuadroResposta

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `projetoId` | Inteiro | — | — |
| `nome` | Texto | — | — |
| `colunas` | Lista de [ColunaResposta](#colunaresposta) | — | — |

### Recomendacao

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `tarefaId` | Inteiro | — | — |
| `ranking` | Lista de [Candidato](#candidato) | — | — |
| `contexto` | [Resposta](#resposta) | — | — |
| `decisao` | Texto | — | — |
| `escolhidoId` | Inteiro | — | — |

### Resposta

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `resposta` | Texto | — | — |
| `fontes` | Lista de [Fonte](#fonte) | — | — |
| `alternativa` | Booleano | — | `false` indica síntese gerada. `true` indica resposta alternativa, geralmente extrativa. |
| `aviso` | Texto | — | Pode ser nulo. |

### TarefaRequestDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `titulo` | Texto | Sim | — |
| `conteudo` | Texto | Não | — |
| `status` | Texto | Sim | `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. |
| `prioridade` | Texto | Sim | `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`. |
| `projetoId` | Inteiro | Sim | — |
| `requisitos` | Texto | Não | Máximo de 10000 caracteres. |
| `tecnologias` | Lista de textos | Não | Máximo de 30 itens. Por item (Máximo de 100 caracteres). |
| `complexidade` | Inteiro | Não | Mínimo de 1. Máximo de 5. |
| `prazo` | Data ISO 8601 | Não | — |
| `responsavelId` | Inteiro | Não | — |

### TarefaResponseDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `titulo` | Texto | — | — |
| `conteudo` | Texto | — | — |
| `status` | Texto | — | `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `CANCELADA`. |
| `prioridade` | Texto | — | `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`. |
| `projetoId` | Inteiro | — | — |
| `createdAt` | Instante ISO 8601 | — | — |
| `updatedAt` | Instante ISO 8601 | — | — |
| `requisitos` | Texto | — | — |
| `tecnologias` | Lista de textos | — | — |
| `complexidade` | Inteiro | — | — |
| `prazo` | Data ISO 8601 | — | — |
| `responsavelId` | Inteiro | — | — |

### TeamMemberDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `id` | Inteiro | — | — |
| `username` | Texto | — | — |
| `email` | Texto | — | — |

### UsuarioLoginRequestDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `email` | Texto | Sim | — |
| `password` | Texto | Sim | — |

### UsuarioRegisterRequestDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `username` | Texto | Sim | Mínimo de 3 caracteres. Máximo de 50 caracteres. |
| `email` | Texto | Sim | — |
| `password` | Texto | Sim | Mínimo de 6 caracteres. Máximo de 2147483647 caracteres. |

### UsuarioResponseDTO

| Campo | Tipo | Obrigatório | Regras |
| --- | --- | --- | --- |
| `papeis` | Lista de textos | — | Por item (`ROLE_MEMBRO` ou `ROLE_ADMINISTRADOR`). |
| `id` | Inteiro | — | — |
| `username` | Texto | — | — |
| `email` | Texto | — | — |
| `createdAt` | Instante ISO 8601 | — | — |
| `updatedAt` | Instante ISO 8601 | — | — |


## Exemplos

### Permissões de gerente no projeto

Ao criar um projeto, a conta entra na equipe como gerente daquele projeto. A resposta inclui `meuPapelNoProjeto: "ROLE_GERENTE"` e `podeGerenciar: true`. Esse acesso vale também para projetos já criados e permite, por exemplo, criar quadros e tarefas em um projeto ativo. Os papéis globais da conta permanecem os mesmos.

O gerente pode transferir a gerência informando o `gerenteId` de um integrante em `PUT /api/projetos/{id}`. Essa permissão vale somente para o projeto atualizado.

### Criar um projeto

Envie a `POST /api/projetos` com JWT.

```json
{
  "nome": "Aurora",
  "descricao": "Projeto de demonstração"
}
```

### Criar uma tarefa

Envie a `POST /api/tarefas` com JWT de gerente da equipe. Use o ID retornado na criação do projeto.

```json
{
  "titulo": "Publicar versão",
  "projetoId": 1,
  "status": "PENDENTE",
  "prioridade": "ALTA",
  "tecnologias": ["Java"],
  "prazo": "2026-12-01"
}
```

### Consultar o RAG

Envie a `POST /api/ia/consultar?projetoId=1` com JWT de integrante do projeto.

```json
{
  "pergunta": "Qual é o código de aprovação da implantação?"
}
```

Exemplo de resposta 200 após indexar o documento do roteiro.

```json
{
  "resposta": "O código de aprovação é AURORA-742.\nFontes: [D1]",
  "fontes": [
    {
      "id": "D1",
      "tipo": "DOCUMENTO",
      "recursoId": 1,
      "titulo": "demo-aurora.md",
      "pagina": null,
      "trecho": "O código de aprovação da implantação é **AURORA-742**.",
      "similaridade": 0.82
    }
  ],
  "alternativa": false,
  "aviso": null
}
```
