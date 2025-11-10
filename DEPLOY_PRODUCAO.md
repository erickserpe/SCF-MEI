# 🚀 GUIA DE DEPLOY PARA PRODUÇÃO - ElloMEI

Este documento contém instruções detalhadas para fazer o deploy da aplicação ElloMEI em ambiente de produção.

---

## 📋 PRÉ-REQUISITOS

### Servidor
- ✅ Ubuntu 20.04 LTS ou superior (recomendado)
- ✅ Mínimo 2GB RAM (4GB recomendado)
- ✅ Mínimo 20GB de disco
- ✅ Acesso root ou sudo

### Software Necessário
- ✅ Docker 20.10+
- ✅ Docker Compose 2.0+
- ✅ Nginx 1.18+
- ✅ Certbot (Let's Encrypt)
- ✅ Git

### Domínio
- ✅ Domínio registrado (ex: ellomei.com.br)
- ✅ DNS configurado apontando para o IP do servidor

---

## 🔧 PASSO 1: PREPARAR O SERVIDOR

### 1.1 Atualizar o Sistema
```bash
sudo apt update
sudo apt upgrade -y
```

### 1.2 Instalar Docker
```bash
# Remover versões antigas
sudo apt remove docker docker-engine docker.io containerd runc

# Instalar dependências
sudo apt install -y apt-transport-https ca-certificates curl software-properties-common

# Adicionar repositório oficial do Docker
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Adicionar usuário ao grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Verificar instalação
docker --version
docker compose version
```

### 1.3 Instalar Nginx
```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 1.4 Instalar Certbot (Let's Encrypt)
```bash
sudo apt install -y certbot python3-certbot-nginx
```

---

## 📦 PASSO 2: CLONAR O REPOSITÓRIO

```bash
# Criar diretório para a aplicação
sudo mkdir -p /opt/ellomei
sudo chown $USER:$USER /opt/ellomei
cd /opt/ellomei

# Clonar o repositório
git clone https://github.com/seu-usuario/ElloMEI.git .

# Ou fazer upload dos arquivos via SCP/SFTP
```

---

## 🔐 PASSO 3: CONFIGURAR VARIÁVEIS DE AMBIENTE

### 3.1 Criar arquivo .env
```bash
cd /opt/ellomei
cp .env.example .env
nano .env
```

### 3.2 Configurar variáveis OBRIGATÓRIAS

**⚠️ IMPORTANTE: Use senhas fortes e únicas!**

```bash
# ===================================
# BANCO DE DADOS
# ===================================
MYSQL_ROOT_PASSWORD=SuaSenhaRootMuitoForte123!@#
MYSQL_DATABASE=ellomei_db
MYSQL_USER=ellomei_user
MYSQL_PASSWORD=SuaSenhaUserMuitoForte456!@#
DB_HOST=mysql

# ===================================
# MERCADO PAGO (PRODUÇÃO)
# ===================================
# Obtenha em: https://www.mercadopago.com.br/developers/panel/app
MERCADOPAGO_ACCESS_TOKEN=APP_USR-seu-token-de-producao-aqui
MERCADOPAGO_PUBLIC_KEY=APP_USR-sua-public-key-de-producao-aqui
MERCADOPAGO_WEBHOOK_SECRET=sua-chave-secreta-webhook

# ===================================
# EMAIL (GOOGLE WORKSPACE / GMAIL)
# ===================================
# Gere uma senha de app em: https://myaccount.google.com/apppasswords
MAIL_USERNAME=noreply@seudominio.com.br
MAIL_PASSWORD=sua-senha-de-app-16-caracteres
MAIL_FROM_NAME=ElloMEI - Sistema de Controle Financeiro

# ===================================
# APLICAÇÃO
# ===================================
APP_BASE_URL=https://seudominio.com.br
APP_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# ===================================
# SSL/HTTPS
# ===================================
# Deixe false - o Nginx vai gerenciar o SSL
SSL_ENABLED=false
```

### 3.3 Proteger o arquivo .env
```bash
chmod 600 .env
```

---

## 🗄️ PASSO 4: CONFIGURAR O BANCO DE DADOS

### 4.1 Iniciar apenas o MySQL
```bash
docker compose up -d mysql
```

### 4.2 Aguardar o MySQL iniciar
```bash
docker compose logs -f mysql
# Aguarde até ver: "ready for connections"
# Pressione Ctrl+C para sair dos logs
```

### 4.3 Verificar saúde do MySQL
```bash
docker compose ps
# O status deve ser "healthy"
```

---

## 🌐 PASSO 5: CONFIGURAR NGINX E SSL

### 5.1 Copiar configuração do Nginx
```bash
sudo cp nginx.conf /etc/nginx/sites-available/ellomei
sudo ln -s /etc/nginx/sites-available/ellomei /etc/nginx/sites-enabled/
```

### 5.2 Editar a configuração
```bash
sudo nano /etc/nginx/sites-available/ellomei
```

**Substitua:**
- `seudominio.com.br` → seu domínio real
- `proxy_pass http://app:8080` → `proxy_pass http://localhost:8080` (se não usar Docker network)

### 5.3 Remover configuração padrão
```bash
sudo rm /etc/nginx/sites-enabled/default
```

### 5.4 Testar configuração
```bash
sudo nginx -t
```

### 5.5 Obter certificado SSL (Let's Encrypt)
```bash
sudo certbot --nginx -d seudominio.com.br -d www.seudominio.com.br
```

**Siga as instruções:**
1. Digite seu email
2. Aceite os termos
3. Escolha se quer compartilhar email (opcional)
4. Escolha opção 2 (redirecionar HTTP para HTTPS)

### 5.6 Reiniciar Nginx
```bash
sudo systemctl restart nginx
```

### 5.7 Verificar renovação automática
```bash
sudo certbot renew --dry-run
```

---

## 🚀 PASSO 6: INICIAR A APLICAÇÃO

### 6.1 Build e iniciar com Docker Compose
```bash
cd /opt/ellomei
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

### 6.2 Acompanhar os logs
```bash
docker compose logs -f app
```

**Aguarde até ver:**
```
Started EllomeiApplication in X.XXX seconds
```

### 6.3 Verificar status dos containers
```bash
docker compose ps
```

**Todos devem estar "healthy":**
- ellomei-mysql
- ellomei-app

---

## ✅ PASSO 7: VERIFICAR O DEPLOY

### 7.1 Testar acesso HTTPS
```bash
curl -I https://seudominio.com.br
```

**Deve retornar:** `HTTP/2 200`

### 7.2 Testar health check
```bash
curl https://seudominio.com.br/actuator/health
```

**Deve retornar:** `{"status":"UP"}`

### 7.3 Acessar no navegador
```
https://seudominio.com.br
```

**Deve carregar a landing page do ElloMEI**

---

## 🔒 PASSO 8: SEGURANÇA ADICIONAL

### 8.1 Configurar Firewall (UFW)
```bash
# Habilitar UFW
sudo ufw enable

# Permitir SSH (IMPORTANTE!)
sudo ufw allow 22/tcp

# Permitir HTTP e HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Verificar status
sudo ufw status
```

### 8.2 Configurar Fail2Ban (proteção contra brute force)
```bash
sudo apt install -y fail2ban
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

### 8.3 Desabilitar acesso direto ao MySQL
```bash
# Editar docker-compose.prod.yml
# Remover ou comentar a seção 'ports' do MySQL
# O MySQL só deve ser acessível internamente via Docker network
```

---

## 📊 PASSO 9: MONITORAMENTO

### 9.1 Configurar Grafana (opcional)
```bash
docker compose -f docker-compose.monitoring.yml up -d
```

**Acesse:** `http://seu-ip:3000`
- Usuário: admin
- Senha: admin (altere na primeira vez)

### 9.2 Configurar alertas de disco
```bash
# Instalar ferramentas de monitoramento
sudo apt install -y htop iotop nethogs
```

---

## 🔄 PASSO 10: BACKUP AUTOMÁTICO

### 10.1 Criar script de backup
```bash
sudo nano /opt/ellomei/scripts/backup-database.sh
```

### 10.2 Tornar executável
```bash
sudo chmod +x /opt/ellomei/scripts/backup-database.sh
```

### 10.3 Configurar cron job
```bash
sudo crontab -e
```

**Adicione:**
```bash
# Backup diário às 2h da manhã
0 2 * * * /opt/ellomei/scripts/backup-database.sh
```

---

## 🆘 TROUBLESHOOTING

### Aplicação não inicia
```bash
# Ver logs detalhados
docker compose logs -f app

# Verificar variáveis de ambiente
docker compose config

# Reiniciar containers
docker compose restart
```

### Erro de conexão com MySQL
```bash
# Verificar se o MySQL está rodando
docker compose ps mysql

# Ver logs do MySQL
docker compose logs mysql

# Testar conexão
docker compose exec mysql mysql -u ellomei_user -p ellomei_db
```

### Certificado SSL não funciona
```bash
# Verificar configuração do Nginx
sudo nginx -t

# Ver logs do Nginx
sudo tail -f /var/log/nginx/error.log

# Renovar certificado manualmente
sudo certbot renew --force-renewal
```

---

## 📝 CHECKLIST FINAL

Antes de considerar o deploy completo, verifique:

- [ ] ✅ Aplicação acessível via HTTPS
- [ ] ✅ Certificado SSL válido (cadeado verde no navegador)
- [ ] ✅ Redirecionamento HTTP → HTTPS funcionando
- [ ] ✅ Banco de dados com senha forte
- [ ] ✅ Variáveis de ambiente configuradas
- [ ] ✅ Backup automático configurado
- [ ] ✅ Firewall habilitado
- [ ] ✅ Logs sendo gerados corretamente
- [ ] ✅ Health check respondendo
- [ ] ✅ Email de verificação funcionando
- [ ] ✅ Integração Mercado Pago testada
- [ ] ✅ Monitoramento configurado (opcional)

---

## 🎉 DEPLOY CONCLUÍDO!

Sua aplicação ElloMEI está agora rodando em produção com:
- ✅ HTTPS/SSL configurado
- ✅ Banco de dados seguro
- ✅ Backup automático
- ✅ Monitoramento ativo
- ✅ Firewall configurado

**Próximos passos:**
1. Criar primeiro usuário de teste
2. Testar fluxo completo de registro
3. Testar integração com Mercado Pago
4. Configurar domínio de email personalizado
5. Divulgar para os primeiros usuários! 🚀

---

**Suporte:** Para dúvidas, consulte a documentação ou abra uma issue no GitHub.

