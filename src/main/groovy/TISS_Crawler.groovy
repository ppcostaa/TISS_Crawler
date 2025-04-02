package groovy

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import groovy.Configuracoes
import groovy.EmailService

class TISS_Crawler {

    void runTasks() {
        createDirectories()

        def files = [:]

        try {
            files.communication = downloadCommunicationComponent()
            files.history = processVersionHistory()
            files.errors = downloadErrorTable()

            println "\nTodas as tarefas foram concluídas com sucesso."

            if (new File(Configuracoes.EMAILS_FILE).exists()) {
                new EmailService().sendReport(files.values() as List)
            }
        } catch (Exception e) {
            println "\nErro durante a execução das tarefas: ${e.message}"
            e.printStackTrace()
        }
    }

    private void createDirectories() {
        def downloadDir = new File(Configuracoes.DOWNLOAD_DIR)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
            println "Diretório de downloads criado em: ${downloadDir.absolutePath}"
        } else {
            println "Diretório de downloads já existe em: ${downloadDir.absolutePath}"
        }
    }

    private String downloadCommunicationComponent() {
        println "\nIniciando Task 1 - Download do Componente de Comunicação"

        def doc = parsePage(Configuracoes.TISS_CURRENT_VERSION_URL)
        println "Página carregada: ${doc.title()}"

        def table = doc.select('table').first()
        if (!table) {
            throw new Exception("Tabela de componentes não encontrada!")
        }

        def link = table.select('a:contains(Componente de Comunicação)').first()
        if (!link) {
            throw new Exception("Link do componente de comunicação não encontrado!")
        }

        println "Link encontrado: '${link.text()}' -> ${link.attr('abs:href')}"

        return downloadFile(
                url: link.attr('abs:href'),
                prefix: 'Componente_Comunicacao',
                taskName: 'Task 1'
        )
    }

    private String processVersionHistory() {
        println "\nIniciando Task 2 - Coleta do Histórico de Versões"

        def doc = parsePage(Configuracoes.VERSION_HISTORY_URL)
        println "Acessando URL do histórico: ${Configuracoes.VERSION_HISTORY_URL}"

        def history = []

        def table = doc.select('table').first()
        if (!table) {
            throw new Exception("Tabela de histórico não encontrada!")
        }

        table.select('tbody tr').each { row ->
            def cols = row.select('td')
            if (cols.size() >= 3) {
                def competencia = cols[0].text().trim()
                if (competencia.contains("/") && competencia.split("/")[1].toInteger() >= 2016) {
                    history.add([
                            competencia: competencia,
                            publicacao: cols[1].text().trim(),
                            vigencia: cols[2].text().trim()
                    ])
                }
            }
        }

        println "Encontrados ${history.size()} registros a partir de jan/2016"

        if (history.isEmpty()) {
            throw new Exception("Nenhum registro de histórico encontrado a partir de jan/2016")
        }

        def csvFile = "${Configuracoes.DOWNLOAD_DIR}/historico_versoes.csv"
        new File(csvFile).withWriter { writer ->
            writer.writeLine('Competência;Publicação;Início Vigência')
            history.each { writer.writeLine("${it.competencia};${it.publicacao};${it.vigencia}") }
        }

        println "Histórico salvo em: ${csvFile}"
        return csvFile
    }

    private String downloadErrorTable() {
        println "\nIniciando Task 3 - Download da Tabela de Erros"

        def doc = parsePage(Configuracoes.RELATED_TABLES_URL)
        println "Acessando URL das tabelas relacionadas: ${Configuracoes.RELATED_TABLES_URL}"

        def link = doc.select('a:contains(Tabela de erros no envio para a ANS)').first()
        if (!link) {
            throw new Exception("Link da tabela de erros não encontrado!")
        }

        println "Link encontrado: '${link.text()}' -> ${link.attr('abs:href')}"

        return downloadFile(
                url: link.attr('abs:href'),
                prefix: 'Tabela_Erros',
                taskName: 'Task 3'
        )
    }

    private Document parsePage(String url) {
        println "Acessando: ${url}"
        try {
            String html = fetchHtml(url)
            Document doc = Jsoup.parse(html, url)
            println "Título da página: ${doc.title()}"
            return doc
        } catch (Exception e) {
            println "Erro ao acessar ${url}: ${e.message}"
            throw e
        }
    }

    private String fetchHtml(String url) {
        def httpClient = HttpClients.createDefault()
        try {
            def httpGet = new HttpGet(url)
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            def response = httpClient.execute(httpGet)
            try {
                if (response.statusLine.statusCode != 200) {
                    throw new Exception("Erro HTTP: ${response.statusLine.statusCode} ${response.statusLine.reasonPhrase}")
                }
                return EntityUtils.toString(response.entity)
            } finally {
                response.close()
            }
        } finally {
            httpClient.close()
        }
    }

    private String downloadFile(Map params) {
        println "${params.taskName}: Baixando arquivo de ${params.url}"

        def fileName = params.url.split('/').last()
        def outputFile = new File("${Configuracoes.DOWNLOAD_DIR}/${params.prefix}_${fileName}")

        def httpClient = HttpClients.createDefault()
        try {
            def httpGet = new HttpGet(params.url)
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            def response = httpClient.execute(httpGet)
            try {
                if (response.statusLine.statusCode != 200) {
                    throw new Exception("Erro HTTP: ${response.statusLine.statusCode} ${response.statusLine.reasonPhrase}")
                }

                outputFile.parentFile.mkdirs()

                Files.copy(
                        response.entity.content,
                        outputFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                )

                println "${params.taskName}: Arquivo salvo em ${outputFile.absolutePath}"
                return outputFile.absolutePath
            } finally {
                response.close()
            }
        } finally {
            httpClient.close()
        }
    }
}
