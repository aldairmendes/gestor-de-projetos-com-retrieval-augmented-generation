# Testes automatizados

Execute a suíte completa com Java 21 e Maven:

```bash
mvn test
```

Os testes de integração usam Spring Boot, MockMvc, os filtros reais de segurança, serviços e JPA com H2 em memória. Não exigem Docker, PostgreSQL, credenciais locais nem serviços de IA. As configurações de banco e JWT de teste estão em `src/test/resources/application-test.properties`, carregadas explicitamente pela classe base, com precedência sobre as configurações da aplicação. Os dados de cada teste de integração são revertidos por transação; IDs são obtidos das entidades persistidas.

## Funcionalidades verificadas

| Área | Cenários principais | Classes |
| --- | --- | --- |
| Autenticação | Cadastro, hash BCrypt, papel mínimo, duplicidade, validação, login, JWT real, token expirado/adulterado e conta removida | `AuthControllerTest` |
| Papéis e privacidade | Matriz de permissões, isolamento por equipe, perfil próprio, rejeição de promoção indevida, promoção/rebaixamento usando JWT existente | `AccessPolicyTest`, `HttpRoleAccessTest`, `UsuarioAuthorizationTest`, `UsuarioControllerTest`, `ProjetoControllerTest` |
| Projetos | Criação, listagem filtrada, consulta, atualização, substituição de equipe, arquivamento/desarquivamento, exclusão em cascata e erros | `ProjetoControllerTest` |
| Tarefas | CRUD, listagem por projeto/usuário, atualização exclusiva de status, transferência autorizada, transferência indevida, projeto arquivado e validação de enums | `TarefaControllerTest`, `TarefaAuthorizationTest` |
| Equipes | Definição/substituição, consulta, inclusão idempotente, remoção, persistência dos vínculos, perda de acesso, criador obrigatório, saída do gerente, IDs inválidos e projeto arquivado | `EquipeControllerIntegrationTest` |
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

Os testes antigos foram adaptados às rotas e respostas atuais: JWT como texto puro, papéis globais e criação/gestão de projetos e tarefas reservada a gerentes integrantes da equipe. Nenhum teste foi desativado para permitir a execução da suíte.
