import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static final String ARQUIVO_CHUVAS = "historico_chuvas.csv";
    private static final String ARQUIVO_UMIDADE = "mapa_umidade.csv";
    private static final String ARQUIVO_RELATORIO = "relatorio_fazenda.txt";

    private static final String[] DIAS_SEMANA = {
            "Segunda-feira", "Terça-feira", "Quarta-feira",
            "Quinta-feira", "Sexta-feira", "Sábado", "Domingo"
    };

    private static final char[] LINHAS_TALHAO = {'A', 'B', 'C', 'D'};

    private static double[] chuvas = new double[7];
    private static double[][] umidade = new double[4][4];
    private static boolean talhoesCadastrados = false;

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        scanner.useLocale(Locale.US);

        menuPrincipal();
        scanner.close();
    }

    public static void menuPrincipal() {
        int opcao;

        do {
            System.out.println("\n===== AGROJAVA SMARTFARM & ANALYTICS =====");
            System.out.println("1) Cadastrar Chuvas da Semana (Analytics)");
            System.out.println("2) Consultar Histórico de Chuvas");
            System.out.println("3) Dashboard Climático (Semana + Chuvosa & Indicadores)");
            System.out.println("4) Cadastrar Umidade dos Talhões (SmartFarm)");
            System.out.println("5) Exibir Mapa do Campo");
            System.out.println("6) Relatório de Alertas de Irrigação");
            System.out.println("7) Relatório Gerencial da Fazenda (e Exportar TXT)");
            System.out.println("8) Sair");
            System.out.print("Escolha uma opção: ");

            while (!scanner.hasNextInt()) {
                System.out.print("Entrada inválida. Digite um número: ");
                scanner.next();
            }
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    cadastrarChuvas();
                    break;
                case 2:
                    consultarHistoricoChuvas();
                    break;
                case 3:
                    gerarDashboardClimatico();
                    break;
                case 4:
                    cadastrarTalhoes();
                    break;
                case 5:
                    exibirMapaCampo();
                    break;
                case 6:
                    gerarRelatorioIrrigacao();
                    break;
                case 7:
                    gerarRelatorioGerencial();
                    break;
                case 8:
                    System.out.println("\nEncerrando o AgroJava...");
                    break;
                default:
                    System.out.println("\nOpção inválida!");
            }
        } while (opcao != 8);
    }

    public static void cadastrarChuvas() {
        System.out.println("\n--- REGISTRO DE SEMANA AGRÍCOLA ---");
        System.out.print("Digite a data da semana (ex: 10/11/2026): ");
        String dataSemana = scanner.nextLine().trim();

        double total = 0;
        for (int i = 0; i < 7; i++) {
            System.out.print("Digite o volume de chuva de " + DIAS_SEMANA[i] + " (em mm): ");
            chuvas[i] = scanner.nextDouble();
            total += chuvas[i];
        }
        scanner.nextLine(); 

        double media = total / 7.0;

        salvarHistoricoChuvas(dataSemana, chuvas, media, total);
        System.out.printf("Semana registrada com sucesso! Média: %.1f mm | Total: %.1f mm%n", media, total);
    }

    public static void salvarHistoricoChuvas(String data, double[] chuvasSemana, double media, double total) {
        File arquivo = new File(ARQUIVO_CHUVAS);
        boolean arquivoNovo = !arquivo.exists() || arquivo.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo, true))) {
            if (arquivoNovo) {
                writer.write("data,seg,ter,qua,qui,sex,sab,dom,media,total");
                writer.newLine();
            }

            String linha = String.format(Locale.US,
                    "%s,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f",
                    data,
                    chuvasSemana[0], chuvasSemana[1], chuvasSemana[2],
                    chuvasSemana[3], chuvasSemana[4], chuvasSemana[5],
                    chuvasSemana[6], media, total
            );

            writer.write(linha);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Erro ao salvar histórico de chuvas: " + e.getMessage());
        }
    }

    public static void consultarHistoricoChuvas() {
        File arquivo = new File(ARQUIVO_CHUVAS);
        if (!arquivo.exists()) {
            System.out.println("\nNenhum histórico de chuvas encontrado.");
            return;
        }

        System.out.println("\n--- HISTÓRICO DE CHUVAS ---");
        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            String linha = reader.readLine(); 
            boolean temRegistros = false;

            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;

                String[] colunas = linha.split(",");
                if (colunas.length >= 10) {
                    temRegistros = true;
                    System.out.println("Semana: " + colunas[0]);
                    System.out.println("Média: " + colunas[8] + " mm");
                    System.out.println("Total: " + colunas[9] + " mm");
                    System.out.println("---------------------------");
                }
            }

            if (!temRegistros) {
                System.out.println("O arquivo de histórico está vazio.");
            }
        } catch (IOException e) {
            System.out.println("Erro ao ler histórico de chuvas: " + e.getMessage());
        }
    }

    public static void gerarDashboardClimatico() {
        File arquivo = new File(ARQUIVO_CHUVAS);
        if (!arquivo.exists()) {
            System.out.println("\nNenhum dado climático cadastrado para gerar o dashboard.");
            return;
        }

        int qtdSemanas = 0;
        double volumeAcumulado = 0;
        double somaMedias = 0;

        double maiorChuva = -1;
        String dataMaiorChuva = "";

        double menorChuva = Double.MAX_VALUE;
        String dataMenorChuva = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {
            reader.readLine(); 
            String linha;

            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;

                String[] colunas = linha.split(",");
                if (colunas.length >= 10) {
                    String data = colunas[0];
                    double mediaSemana = Double.parseDouble(colunas[8]);
                    double totalSemana = Double.parseDouble(colunas[9]);

                    qtdSemanas++;
                    volumeAcumulado += totalSemana;
                    somaMedias += mediaSemana;

                    if (totalSemana > maiorChuva) {
                        maiorChuva = totalSemana;
                        dataMaiorChuva = data;
                    }
                    if (totalSemana < menorChuva) {
                        menorChuva = totalSemana;
                        dataMenorChuva = data;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Erro ao processar dados climáticos: " + e.getMessage());
            return;
        }

        if (qtdSemanas == 0) {
            System.out.println("\nNenhuma semana registrada.");
            return;
        }

        double mediaGeral = somaMedias / qtdSemanas;

        System.out.println("\n===== DASHBOARD CLIMÁTICO =====");
        System.out.println("Semanas cadastradas: " + qtdSemanas);
        System.out.printf("Volume acumulado: %.1f mm%n", volumeAcumulado);
        System.out.printf("Semana mais chuvosa: %s (%.1f mm)%n", dataMaiorChuva, maiorChuva);
        System.out.printf("Semana mais seca: %s (%.1f mm)%n", dataMenorChuva, menorChuva);
        System.out.printf("Média geral: %.1f mm%n", mediaGeral);
    }

    public static void cadastrarTalhoes() {
        System.out.println("\n--- UMIDADE DOS TALHÕES (MATRIZ 4x4) ---");

        for (int i = 0; i < umidade.length; i++) {
            for (int j = 0; j < umidade[i].length; j++) {
                String codigoTalhao = "" + LINHAS_TALHAO[i] + (j + 1);
                System.out.print("Digite a umidade do talhão " + codigoTalhao + " (%): ");
                umidade[i][j] = scanner.nextDouble();
            }
        }
        scanner.nextLine(); 
        talhoesCadastrados = true;

        salvarMapaTalhoes();
        System.out.println("\nDados dos talhões cadastrados e exportados para '" + ARQUIVO_UMIDADE + "'!");
    }

    public static String classificarTalhao(double valorUmidade) {
        if (valorUmidade < 20.0) {
            return "Crítico";
        } else if (valorUmidade <= 30.0) {
            return "Atenção";
        } else {
            return "Adequado";
        }
    }

    public static void salvarMapaTalhoes() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_UMIDADE))) {
            writer.write("talhao,umidade,status");
            writer.newLine();

            for (int i = 0; i < umidade.length; i++) {
                for (int j = 0; j < umidade[i].length; j++) {
                    String codigoTalhao = "" + LINHAS_TALHAO[i] + (j + 1);
                    double valor = umidade[i][j];
                    String status = classificarTalhao(valor);

                    writer.write(String.format(Locale.US, "%s,%.1f,%s", codigoTalhao, valor, status));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Erro ao salvar mapa de umidade: " + e.getMessage());
        }
    }

    public static void exibirMapaCampo() {
        if (!talhoesCadastrados) {
            System.out.println("\nCadastre a umidade dos talhões primeiro! Utilize a opção 4.");
            return;
        }

        System.out.println("\n--- MAPA DE UMIDADE DO CAMPO ---");
        System.out.println("\t1\t2\t3\t4");
        for (int i = 0; i < umidade.length; i++) {
            System.out.print(LINHAS_TALHAO[i] + "\t");
            for (int j = 0; j < umidade[i].length; j++) {
                System.out.printf("%.1f%%\t", umidade[i][j]);
            }
            System.out.println();
        }
    }

    public static void gerarRelatorioIrrigacao() {
        if (!talhoesCadastrados) {
            System.out.println("\nCadastre a umidade dos talhões primeiro! Utilize a opção 4.");
            return;
        }

        System.out.println("\n===== ALERTAS DE IRRIGAÇÃO =====");
        int totalAlertas = 0;

        for (int i = 0; i < umidade.length; i++) {
            for (int j = 0; j < umidade[i].length; j++) {
                double valor = umidade[i][j];
                String status = classificarTalhao(valor);

                if (status.equals("Crítico") || status.equals("Atenção")) {
                    String codigoTalhao = "" + LINHAS_TALHAO[i] + (j + 1);
                    System.out.printf("%s - %s (%.1f%%)%n", codigoTalhao, status, valor);
                    totalAlertas++;
                }
            }
        }

        if (totalAlertas == 0) {
            System.out.println("Nenhum talhão necessita de irrigação no momento.");
        } else {
            System.out.println("Total de alertas: " + totalAlertas);
        }
    }

    public static void gerarRelatorioGerencial() {
        if (!talhoesCadastrados) {
            System.out.println("\nCadastre a umidade dos talhões primeiro! Utilize a opção 4.");
            return;
        }

        double somaUmidade = 0;
        int totalTalhoes = 16;
        int talhoesCriticos = 0;
        int talhoesAdequados = 0;
        int talhoesNecessitamIrrigacao = 0;

        double menorUmidade = Double.MAX_VALUE;
        String talhaoMaisSeco = "";

        double maiorUmidade = -1;
        String talhaoMaisUmido = "";

        for (int i = 0; i < umidade.length; i++) {
            for (int j = 0; j < umidade[i].length; j++) {
                double valor = umidade[i][j];
                String codigoTalhao = "" + LINHAS_TALHAO[i] + (j + 1);
                String status = classificarTalhao(valor);

                somaUmidade += valor;

                if (status.equals("Crítico")) {
                    talhoesCriticos++;
                    talhoesNecessitamIrrigacao++;
                } else if (status.equals("Atenção")) {
                    talhoesNecessitamIrrigacao++;
                } else {
                    talhoesAdequados++;
                }

                if (valor < menorUmidade) {
                    menorUmidade = valor;
                    talhaoMaisSeco = codigoTalhao;
                }
                if (valor > maiorUmidade) {
                    maiorUmidade = valor;
                    talhaoMaisUmido = codigoTalhao;
                }
            }
        }

        double mediaGeral = somaUmidade / totalTalhoes;
        double percentualIrrigacao = ((double) talhoesNecessitamIrrigacao / totalTalhoes) * 100.0;
        String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("===== RELATORIO DA FAZENDA =====\n");
        relatorio.append("Data: ").append(dataAtual).append("\n");
        relatorio.append(String.format("Media Geral: %.0f%%\n", mediaGeral));
        relatorio.append("Talhoes Criticos: ").append(talhoesCriticos).append("\n");
        relatorio.append("Talhoes Adequados: ").append(talhoesAdequados).append("\n");
        relatorio.append(String.format("Necessidade de Irrigacao: %.0f%%\n", percentualIrrigacao));
        relatorio.append(String.format("Talhao mais seco: %s - %.0f%%\n", talhaoMaisSeco, menorUmidade));
        relatorio.append(String.format("Talhao mais umido: %s - %.0f%%\n", talhaoMaisUmido, maiorUmidade));

        System.out.println("\n" + relatorio);

        System.out.print("Deseja exportar este relatório para '" + ARQUIVO_RELATORIO + "'? (S/N): ");
        String resposta = scanner.nextLine().trim();
        if (resposta.equalsIgnoreCase("S")) {
            exportarRelatorio(relatorio.toString());
        }
    }

    public static void exportarRelatorio(String conteudo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARQUIVO_RELATORIO))) {
            writer.write(conteudo);
            System.out.println("Relatório exportado com sucesso para '" + ARQUIVO_RELATORIO + "'!");
        } catch (IOException e) {
            System.out.println("Erro ao exportar relatório: " + e.getMessage());
        }
    }
}