package groovy
import groovy.TISS_Crawler
class Main {
    static void main(String[] args) {
        println "Iniciando TISS Crawler"


        new TISS_Crawler().runTasks()
    }
}