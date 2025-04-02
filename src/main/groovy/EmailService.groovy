package groovy

import javax.mail.Session
import javax.mail.Message
import javax.mail.Transport
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Multipart
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import groovy.Configuracoes

class EmailService {

    void sendReport(List<String> attachments) {
        println "\nIniciando envio de e-mails..."

        def emails = loadEmails()
        if (emails.empty) {
            println "Nenhum e-mail cadastrado para notificação"
            return
        }

        def session = createSession()
        emails.each { email ->
            try {
                sendEmail(session, email, attachments)
            } catch (Exception e) {
                println "Erro ao enviar para ${email}: ${e.message}"
            }
        }
    }

    private List<String> loadEmails() {
        def file = new File(Configuracoes.EMAILS_FILE)
        file.exists() ? file.readLines().findAll { it } : []
    }

    private Session createSession() {
        Properties properties = new Properties()
        properties.put("mail.smtp.host", Configuracoes.SMTP_HOST)
        properties.put("mail.smtp.port", Configuracoes.SMTP_PORT)
        properties.put("mail.smtp.auth", "true")
        properties.put("mail.smtp.starttls.enable", "true")

        return Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Configuracoes.EMAIL_FROM, Configuracoes.EMAIL_PASS)
            }
        })
    }

    private void sendEmail(Session session, String to, List<String> attachments) {
        Message message = new MimeMessage(session)
        message.setFrom(new InternetAddress(Configuracoes.EMAIL_FROM))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
        message.setSubject("[TISS] Relatório de Atualização")

        Multipart multipart = new MimeMultipart()

        MimeBodyPart textPart = new MimeBodyPart()
        textPart.setText("""Relatório de Atualização TISS
                
        Arquivos anexos:
        - Componente de Comunicação
        - Histórico de Versões
        - Tabela de Erros
                
        Atualizado em: ${new Date()}""")
        multipart.addBodyPart(textPart)

        attachments.each { filePath ->
            File file = new File(filePath)
            if (file.exists()) {
                MimeBodyPart attachmentPart = new MimeBodyPart()
                attachmentPart.attachFile(file)
                multipart.addBodyPart(attachmentPart)
            } else {
                println "Arquivo não encontrado: ${filePath}"
            }
        }

        message.setContent(multipart)

        Transport.send(message)
        println "E-mail enviado com sucesso para: ${to}"
    }

    static void addEmail(String email) {
        if (!new File(Configuracoes.EMAILS_FILE).exists()) {
            new File(Configuracoes.EMAILS_FILE).createNewFile()
        }
        new File(Configuracoes.EMAILS_FILE) << "${email}\n"
        println "E-mail ${email} adicionado com sucesso!"
    }

    static void removeEmail(String email) {
        def file = new File(Configuracoes.EMAILS_FILE)
        if (file.exists()) {
            def emails = file.readLines()
            file.withWriter { writer ->
                emails.each { if (it != email) writer.writeLine(it) }
            }
            println "E-mail ${email} removido com sucesso!"
        } else {
            println "Arquivo de e-mails não encontrado."
        }
    }

    static void listEmails() {
        def file = new File(Configuracoes.EMAILS_FILE)
        if (file.exists()) {
            def emails = file.readLines()
            println "E-mails cadastrados:"
            if (emails.isEmpty()) {
                println "Nenhum e-mail cadastrado."
            } else {
                emails.each { println "- ${it}" }
            }
        } else {
            println "Arquivo de e-mails não encontrado."
        }
    }
}
