package it.giovannimoretti;


import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.util.*;

public class CoNLL_Main {

    private enum Mode {
        POS,
        EDGES,
        DEPREL,
        MORPH,
        LEMMA
    }

    public static void main(String[] args) {

        CommandLineParser parser = new PosixParser();
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");


        options.addOption(OptionBuilder.withLongOpt("mode").withDescription("calculate irr on specific ConLL-U field").withArgName("LEMMA | POS | MORPH | EDGES | DEPREL").hasArg().create("m"));

        CommandLine cline = null;
        List<String> filePaths = new ArrayList<>();
        String generalHelpDescr = "CoNLL-U IRR [-m|-h] <conllu file rate 1> <conllu file rate 2> ...";
        Mode mode = null;
        try {
            cline = parser.parse(options, args);
            if (cline.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(500);
                formatter.printHelp(generalHelpDescr, options);
                System.exit(0);
            }
            if (cline.hasOption("mode")) {
                try {
                    mode = Mode.valueOf(cline.getOptionValue("mode").toUpperCase());
                } catch (Exception e) {
                    System.out.println("\nerror: Wrong value for the option mode\n");
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.setWidth(500);
                    formatter.printHelp(generalHelpDescr, options);
                    System.exit(1);
                }
            }else if(!cline.hasOption("mode")){
                System.out.println("\nerror: mode parameter is required\n");
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(500);
                formatter.printHelp(generalHelpDescr, options);
                System.exit(1);
            }


            filePaths = cline.getArgList();
            if (filePaths.size() < 2){
                System.out.println("\nerror: Specify at least two files\n");
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(500);
                formatter.printHelp(generalHelpDescr, options);
                System.exit(1);
            }

        } catch (Exception e) {

        }


        List<CoNLL_U_File> files = new ArrayList<>();

        for (String documentPath : filePaths) {

            StringBuffer f = new StringBuffer();

            try (BufferedReader br = new BufferedReader(new FileReader(documentPath))) {

                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        f.append(line + "\n");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            CoNLL_U_File fC = new CoNLL_U_File(f.toString());

            files.add(fC);

        }

        // produce arcs file
        Integer sentenceNumber = files.get(0).getCoNNL_U_Sentences().size();
        Integer raterIndex = 1;
        List<List<String>> lines = new ArrayList<>();

        SortedSet<String> allFeatures = new TreeSet<>();

        for (CoNLL_U_File f : files) {
            if (lines.size() == 0) {
                lines.add(new ArrayList<>());
            }
            lines.get(0).add("rater" + raterIndex);

            if (f.getCoNNL_U_Sentences().size() != sentenceNumber) {
                System.err.println("Mismatch in sentence number.");
                System.exit(1);
            }

            Integer lineNumber = 1;
            Integer sentNumber = 1;


            for (String sentence : f.getCoNNL_U_Sentences()) {
                for (String line : sentence.split("\n")) {
                    String[] lineItems = line.split("\t");
                    if (lineItems[0].contains("-")) {
                        continue;
                    }
                    if (lines.size() == lineNumber) {
                        lines.add(new ArrayList<>());
                    }
                    if (mode == Mode.POS) {
                        lines.get(lineNumber).add(lineItems[3]);
                    } else if (mode == Mode.LEMMA) {
                        lines.get(lineNumber).add(lineItems[2]);
                    } else if (mode == Mode.EDGES) {
                        // only edge
                        lines.get(lineNumber).add(lineItems[0] + "_" + lineItems[6]);
                    } else if (mode == Mode.MORPH) {
                        String morph = lineItems[5].trim().length() == 0 ? "_" : lineItems[5].trim();
                        List<String> features = new ArrayList<>(Arrays.asList(morph.replace("_", "").split("\\|")));

                        for (String ft : features){
                            if (ft.split("=")[0].length() > 0) {
                                allFeatures.add(ft.split("=")[0]);
                            }
                        }

                        lines.get(lineNumber).add(morph);
                    } else if (mode == Mode.DEPREL) {
                        // edge and deprel together
                        lines.get(lineNumber).add(lineItems[0] + "_" + lineItems[6] + "-" + lineItems[7]);
                    }


                    lineNumber++;
                }
                sentNumber++;
            }
            raterIndex++;
        }
        //System.out.println(lines);

        if (mode == Mode.POS) {
            calculateKappa(lines, false, true);
        }  else if (mode == Mode.LEMMA) {
            calculateKappa(lines, false, false);
        } else if (mode == Mode.MORPH) {
            calculateKappa(lines, false, false);
            System.out.println("-------------");
            for (String feature : allFeatures){
                System.out.print(feature+"\t");
                List<List<String>> copyOfLines  = makeACopyOfList(lines);
                calculateKappa(copyOfLines, false, false, feature);
            }

        }  else if (mode == Mode.EDGES) {
            calculateKappa(lines, false, false);
        } else if (mode == Mode.DEPREL) {
            calculateKappa(lines, true, true);
        }

    }

    public static void calculateKappa(List<List<String>> copyOfLines, boolean onArcs, boolean details, String feature) {
        for (List<String> l : copyOfLines) {

            for (int i = 0; i < l.size(); i++) {

                if (l.get(0).startsWith("rater")) {
                    continue;
                }
                List<String> features = new ArrayList<>(Arrays.asList(l.get(i).replace("_", "").split("\\|")));
                l.set(i,"_");
                for (String f : features){
                    if (f.startsWith(feature+"=")){
                        l.set(i,f);
                    }
                }

            }

        }



        calculateKappa(copyOfLines,false,false);
    }


    public static void calculateKappa(List<List<String>> lines, boolean onArcs, boolean details) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("Raters_CSV.tsv"));
            CSVFormat csvFileFormat = CSVFormat.TDF.withRecordSeparator("\n");
            CSVPrinter printer = new CSVPrinter(writer, csvFileFormat);
            for (List<String> l : lines) {
                if (l.get(0).startsWith("rater")) {
                    printer.printRecord(l);
                    continue;
                }
                if (onArcs) {
                    String arc = l.get(0).split("-")[0];
                    List<String> tmpval = new ArrayList<>();

                    for (String v : l) {
                        if (v.split("-")[0].equals(arc)) {
                            tmpval.add(v.split("-")[1]);
                        } else {
                            continue;
                        }
                    }

                    if (tmpval.size() == lines.get(0).size()) {
                        printer.printRecord(tmpval);
                    }
                } else {
                    printer.printRecord(l);
                }

            }

            printer.flush();
            printer.close();
            writer.close();


            ProcessBuilder builder = new ProcessBuilder("Rscript", "fleiss.r");

            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                if (!details) {
                    if (line.contains("Kappa =")) {
                        System.out.println(line.trim());
                    }
                } else {
                    System.out.println(line.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static List<List<String>> makeACopyOfList (List<List<String>> srcList) {
        List<List<String>> destList = new ArrayList<>();
        for (List<String> l : srcList){
            destList.add(new ArrayList<>(l));
        }

        return destList;

    }

}
