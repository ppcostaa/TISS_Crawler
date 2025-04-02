package groovy

class Configuracoes {
    static final String SMTP_HOST = "smtp.mailtrap.io"
    static final Integer SMTP_PORT = 2525
    static final String EMAIL_FROM = "no-reply@tiss-crawler.com"
    static final String EMAIL_PASS = "seu-codigo"
    static final String EMAILS_FILE = "emails.dat"
    static final String DOWNLOAD_DIR = "Downloads"
    static final String BASE_URL = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss"

    static final String TISS_CURRENT_VERSION_URL = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-marco-2025"
    static final String VERSION_HISTORY_URL = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-historico-das-versoes-dos-componentes-do-padrao-tiss"
    static final String RELATED_TABLES_URL = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-tabelas-relacionadas"

    static {
        try {
            def configFile = new File("config.properties")
            if (configFile.exists()) {
                Properties props = new Properties()
                configFile.withInputStream { props.load(it) }

                def smtpHost = props.getProperty("smtp.host")
                if (smtpHost) SMTP_HOST = smtpHost

                def smtpPort = props.getProperty("smtp.port")
                if (smtpPort) {
                    try {
                        SMTP_PORT = Integer.parseInt(smtpPort)
                    } catch (NumberFormatException e) {
                        println "Valor inválido para smtp.port: ${smtpPort}. Usando padrão: ${SMTP_PORT}"
                    }
                }

                def emailFrom = props.getProperty("email.from")
                if (emailFrom) EMAIL_FROM = emailFrom

                def emailPass = props.getProperty("email.password")
                if (emailPass) EMAIL_PASS = emailPass

                def emailsFile = props.getProperty("emails.file")
                if (emailsFile) EMAILS_FILE = emailsFile

                def downloadDir = props.getProperty("download.dir")
                if (downloadDir) DOWNLOAD_DIR = downloadDir

                def baseUrl = props.getProperty("base.url")
                if (baseUrl) BASE_URL = baseUrl
            }
        } catch (Exception e) {
            println "Erro ao carregar configurações: ${e.message}. Usando valores padrão."
        }

        println "Configurações carregadas:"
        println "- Base URL: ${BASE_URL}"
        println "- Download Dir: ${DOWNLOAD_DIR}"
        println "- SMTP: ${SMTP_HOST}:${SMTP_PORT}"
    }
}