# Testes automatizados

Para executar cenários interativos, consulte o [manual de testes pelo Swagger](MANUAL_TESTES_SWAGGER.md).

Execute a suíte completa com Java 21 e Maven:

```bash
mvn test
```

Use o Java 21: com versões mais novas (ex.: Java 27) a compilação falha no Lombok com `TypeTag :: UNKNOWN`. Se o Java padrão do terminal for outro, aponte o `JAVA_HOME` para o 21 só nesse comando, por exemplo `JAVA_HOME=~/.sdkman/candidates/java/21.0.12.fx-zulu mvn test`.

`QuadroControllerPostgresTest` e `TarefaColaboracaoPostgresTest` usam PostgreSQL via Testcontainers: com o Docker ligado eles rodam; sem Docker são pulados (aparecem como *skipped*) em vez de falhar a suíte.

Os testes de integração usam Spring Boot, MockMvc, os filtros reais de segurança, serviços e JPA com H2 em memória. Não exigem Docker, PostgreSQL, credenciais locais nem serviços de IA. As configurações de banco e JWT de teste estão em `src/test/resources/application-test.properties`, carregadas explicitamente pela classe base, com precedência sobre as configurações da aplicação. Os dados de cada teste de integração são revertidos por transação; IDs são obtidos das entidades persistidas.

## Funcionalidades verificadas

| Área | Cenários principais | Classes |
| --- | --- | --- |
| Autenticação | Cadastro, hash BCrypt, papel mínimo, duplicidade, validação, login, JWT real, token expirado/adulterado e conta removida | `AuthControllerTest` |
| Papéis e privacidade | Matriz de permissões, isolamento por equipe, perfil próprio, rejeição de promoção indevida, promoção/rebaixamento usando JWT existente | `AccessPolicyTest`, `HttpRoleAccessTest`, `UsuarioAuthorizationTest`, `UsuarioControllerTest`, `ProjetoControllerTest` |
| Projetos | Criação, listagem filtrada, consulta, atualização, substituição de equipe, arquivamento/desarquivamento, exclusão em cascata e erros | `ProjetoControllerTest` |
| Tarefas | CRUD, listagem por projeto/usuário, atualização exclusiva de status, transferência autorizada, transferência indevida, projeto arquivado e validação de enums | `TarefaControllerTest`, `TarefaAuthorizationTest` |
| Quadros e colunas | Status vinculado às colunas, criação de tarefa em coluna, movimentação por integrante com histórico, sincronização de coluna ao mudar o status, coluna de outro projeto, projeto arquivado, edição sem perder a coluna e realocação obrigatória ao remover coluna com tarefas | `ColunaTarefaIntegrationTest` |
| Equipes | Definição/substituição, consulta, inclusão idempotente, remoção, persistência dos vínculos, perda de acesso, criador obrigatório, bloqueio da remoção do gerente atual, IDs inválidos e projeto arquivado | `EquipeControllerIntegrationTest` |
| Usuários | Diretório, perfis, criação administrativa, senhas, exclusão e atribuição validada de papéis globais | `UsuarioControllerTest`, `UsuarioAuthorizationTest` |
| Inicialização | Contexto da aplicação, catálogo de papéis idempotente, administrador inicial com hash e papel mínimo, preservação de conta existente | `GestorDeProjetosComRetrievaAugmentedGenerationApplicationTests`, `DataInitializerTest` |
| Swagger | Todas as operações dos controladores REST, descrições, JWT, exemplos, schemas, referências internas, enums, respostas sem corpo e interface pública | `SwaggerControllerTest` |
| IA e RAG | Regras de domínio, isolamento por equipe, alternativas sem provedor, recomendação, feedback, histórico, ingestão com provedor simulado, filtragem de trechos e validação das citações | `IaEAdminControllerTest`, `RagDocumentIntegrationTest` |

Os testes unitários de autorização permanecem independentes de Spring e do banco. O Mockito usa `mock-maker-subclass` porque os doubles atuais não precisam substituir classes finais, evitando a necessidade de anexar um agente à JVM.

## Relatórios

- Resultados JUnit XML e texto: `target/surefire-reports/`.
- Contrato OpenAPI gerado durante o teste: `target/openapi.json`.
- Uma classe: `mvn -Dtest=EquipeControllerIntegrationTest test`.

A suíte verifica o comportamento atual da API. Ela não mede percentual de cobertura e não substitui testes específicos de PostgreSQL, pgvector ou chamadas a modelos de IA.

## Regressões corrigidas durante a ampliação

- Exclusão de tarefa: sincronização da coleção de tarefas do projeto antes da exclusão, impedindo sua persistência novamente por cascata.
- OpenAPI: preservação dos schemas usados nas respostas reutilizáveis, para que referências como `#/components/schemas/ErroApi` continuem válidas no contrato gerado.

Os testes antigos foram adaptados às rotas e respostas atuais: JWT como texto puro, papéis globais e criação de projetos por usuários autenticados e gestão restrita ao gerente de cada projeto. Nenhum teste foi desativado para permitir a execução da suíte.
