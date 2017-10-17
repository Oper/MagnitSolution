package ru.vbaykov.magnit;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    //Максимальное время работы программы
    private static int timeout = 300000;

    public static void main(String[] args) {
        String login = null, pass = null;
        int n = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Login");
            login = reader.readLine();
            System.out.println("Password");
            pass = reader.readLine();
            System.out.println("N число");
            n = Integer.parseInt(String.valueOf(Integer.parseInt(reader.readLine())));

        } catch (IOException e) {
            e.printStackTrace();
        }
        DBworker dBworker = new DBworker();
        dBworker.setUrl("jdbc:postgresql://localhost:5432/TEST");
        dBworker.setLogin(login);
        dBworker.setPass(pass);
        dBworker.setN(n);
        dBworker.start();
        try {
            dBworker.join(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ArrayList<Long> list = dBworker.getList();
        //Работа с xml
        try {

            dBworker.createXML(list, "1.xml");
            dBworker.transform("1.xml", "src/styleXSLT.xsl", "2.xml");
            //parsing 2.xml
            ArrayList<Integer> list1 = (ArrayList<Integer>) dBworker.parsingXML("2.xml");
            int result = 0;
            for (int i = 0; i < list1.size(); i++) {
                result += list1.get(i);
            }
            //Result print
            System.out.println(result);

        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    static class DBworker extends Thread {
        //Данные для подключения к БД
        String url;
        String login;
        String pass;
        //Число N
        int n;
        volatile ArrayList<Long> list = new ArrayList<Long>();

        void setUrl(String url) {
            this.url = url;
        }

        void setLogin(String login) {
            this.login = login;
        }

        void setPass(String pass) {
            this.pass = pass;
        }

        void setN(int n) {
            this.n = n;
        }

        ArrayList<Long> getList() {
            return list;
        }

        @Override
        public void run() {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            try (Connection connection = DriverManager.getConnection(this.url, this.login, this.pass);
                 Statement statement = connection.createStatement()) {
                //Чистим таблицу если не пустая
                statement.executeUpdate("DELETE from TEST");
                //Вставка данных

                for (int y = 1; y < this.n + 1; y++) {
                    statement.executeUpdate("INSERT INTO TEST (FIELD) VALUES (" + y + ")");
                }
                //Получение и сохранение в List
                ResultSet resultSet = statement.executeQuery("SELECT * FROM test");
                while (resultSet.next()) {
                    list.add(resultSet.getLong(1));
                }
                this.interrupt();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        void transform(String dataXML, String inputXSL, String outputXSLT) throws TransformerException {
            TransformerFactory factory = TransformerFactory.newInstance();
            StreamSource xslStream = new StreamSource(inputXSL);
            Transformer transformer = factory.newTransformer(xslStream);
            StreamSource in = new StreamSource(dataXML);
            StreamResult out = new StreamResult(outputXSLT);
            transformer.transform(in, out);
        }

        List parsingXML(String dataXML) {
            ArrayList<Integer> list = new ArrayList<>();
            try {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = documentBuilder.parse(new File(dataXML));

                NodeList children = document.getElementsByTagName("entry");
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    NamedNodeMap attributes = node.getAttributes();
                    Node attribut = attributes.getNamedItem("field");
                    String valueAttribut = attribut.getNodeValue();
                    list.add(Integer.parseInt(valueAttribut));
                }


            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return list;
        }

        void createXML(List<Long> list, String outputXML) {
            try {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();

                Document document = builder.newDocument();
                Element rootElement = document.createElement("entries");
                document.appendChild(rootElement);

                for (int i = 0; i < list.size(); i++) {
                    Element entryElement = document.createElement("entry");
                    rootElement.appendChild(entryElement);

                    Element fieldElement = document.createElement("field");
                    fieldElement.appendChild(document.createTextNode(String.valueOf(list.get(i))));
                    entryElement.appendChild(fieldElement);
                }

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(document);
                StreamResult result1 = new StreamResult(new File(outputXML));
                transformer.transform(source, result1);

            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    }
}
