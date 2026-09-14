# SECURITY INCIDENT — Signing credentials exposure

## Status
`REMEDIATION_REQUIRED`

## Finding
Um arquivo versionado na raiz do repositório continha valores de credenciais de assinatura em texto puro. O arquivo foi removido da árvore ativa durante a adoção do Ideias Standard.

## Impact assessment
- Os valores expostos devem ser tratados como comprometidos.
- Não foi encontrada evidência, nos caminhos esperados verificados, de que o arquivo `.jks` ou uma cópia Base64 do keystore tenha sido versionada.
- A remoção do arquivo atual não apaga o conteúdo do histórico Git já publicado.

## Required human actions
1. Trocar as senhas/segredos de assinatura armazenados no GitHub Actions.
2. Preservar a mesma chave/certificado de assinatura existente quando necessário para manter compatibilidade de atualização do aplicativo já instalado.
3. Não reutilizar os valores que estiveram no repositório.
4. Se houver suspeita de exposição do próprio keystore/chave privada fora dos caminhos verificados, tratar como incidente de chave e seguir o procedimento de rotação aplicável à distribuição do app.

## Repository controls added
- padrões de keystore, `.env` e chaves privadas ignorados por Git;
- release deve falhar quando qualquer secret de assinatura obrigatório estiver ausente;
- CI de validação será separado do release;
- checks de segurança passam a fazer parte do gate da adoção.

## Evidence policy
Este documento nunca deve conter valores de secret, hashes reversíveis de senha, Base64 de keystore ou cópia de chave privada.

## Gate
A adoção pode ficar `AUDIT_READY`, mas publicação de release assinada deve permanecer bloqueada até a ação humana de rotação dos secrets ser concluída e validada.