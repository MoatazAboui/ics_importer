package com.mycompany.webscraping;

import java.io.BufferedWriter;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import org.jsoup.Connection;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;


public class Webscraping {

    public int number = 0;
    //scraping site url
    public String site_url = "https://campus.uni-due.de/lsf/rds?state=wplan&act=Raum&pool=Raum&show=plan&P.subc=plan&raum.dtxt=C98";

    public static void main(String[] args) throws IOException {

        //object creation
        Webscraping webscraping = new Webscraping();

        //by site url, get Document
        Document doc = Jsoup.connect(webscraping.site_url).followRedirects(true).get();
        //get title of site
        String title = doc.title();
        System.out.println("title of site: " + title);

        //drop box data getting
        Elements links = doc.select(".kleinerButton > optgroup").last().select("option");

        for (Element link : links) {
            webscraping.getSchedule(link);
        }
    }

    public void getSchedule(Element link) throws IOException {

        this.number++;
        //get data from select value
        String form_week = link.attr("value");
        //file name
        String filename = link.text().trim();
        //form data
        String form_work = "anzeigen";

        Document indi_doc = Jsoup.connect(this.site_url).data("week", form_week).data("work", form_work).followRedirects(true).post();

        String schedule = indi_doc.select(".tree").attr("href");

        Document doc_schedule = Jsoup.connect(schedule).get();
        String el_schedule = doc_schedule.select("body").html();
        String[] array_schedule = el_schedule.split("\\s+");

        try {
            String title_schedule = ".\\schedule\\" + "schedule" + this.number + ".ics";
            File mySchedule = new File(title_schedule);
            if (mySchedule.createNewFile()) {
                System.out.println("File created: " + mySchedule.getName());
                FileWriter myWriter = new FileWriter(title_schedule);
//                myWriter.write(el_schedule);
                int i = 0;
                for (String row : array_schedule) {
                    i++;
                    if(row.contains("EXDATE")){}
                    else if (row.contains(":")) {
                        if (i == 1) {
                            myWriter.write(row);
                        } else {
                            myWriter.write("\n" + row);
                        }
                    } else {
                        myWriter.write(" "+row);
                    }
                }
                myWriter.close();
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        Elements tables = indi_doc.select(".form > table").attr("border", "1");
        Element table = tables.last();
        this.analyse_data(table, filename);
    }

    public void analyse_data(Element table, String filename) throws IOException {
        Elements trs = table.select("tbody > tr");
        int length = trs.size();
        Element childNode1 = trs.first();
        Elements header = childNode1.select("th");
        int header_num = header.size();
        String[] str_header = new String[]{"Zeit", "", "", "", "", "", "", ""};
        try {
            for (int i = 0; i < header_num; i++) {
                str_header[i + 2] = header.get(i).text().trim();
            }
        } catch (Exception e) {
        }

        String path = ".\\csv\\" + filename.substring(7, 10).trim() + ".csv";

        File myObj = new File(path);
        if (!myObj.exists()) {
            ICsvBeanWriter beanWriter = null;
            beanWriter = new CsvBeanWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-16"), CsvPreference.STANDARD_PREFERENCE);
            // write the header
            beanWriter.writeHeader(str_header);
            try {
                for (int i = 1; i < length; i++) {
                    String[] str_content = new String[]{"", "", "", "", "", "", ""};

                    Elements sub_elements = trs.first().siblingElements().get(i).children();

                    try {
                        for (int j = 0; j < sub_elements.size(); j++) {
                            String fullname_url = sub_elements.get(j).select("a").attr("href");
                            String fullname = "";
                            if (!fullname_url.equals("")) {
                                Document fullname_doc = Jsoup.connect(fullname_url).followRedirects(true).get();
                                fullname = fullname_doc.select("form > h1").first().text();
                                System.out.println("Fullname-----------------" + fullname);
                            }
                            if (sub_elements.get(j).firstElementSibling().toString().equals("<td width=\"0\"></td>")) {
                                if (!fullname.equals("")) {
                                    str_content[j + 1] = fullname + "\n" + fullname_url;
                                } else {
                                    str_content[j + 1] = sub_elements.get(j).text().replaceAll("\u00a0", " ");
                                }
                            } else {
                                if (!fullname.equals("")) {
                                    str_content[j] = fullname + "\n" + fullname_url;
                                } else {
                                    str_content[j] = sub_elements.get(j).text().replaceAll("\u00a0", " ");

                                }
                            }
                        }
                    } catch (Exception e) {

                    }

                    beanWriter.writeHeader(str_content);
                }
            } catch (Exception e) {
            }

            beanWriter.close();
        } else {
            System.out.println("File already exists.");
        }
    }
}
