package team3.database;

import team3.database.DBSearch.ActiveSmiSearch;
import team3.database.DBSearch.DBSearch;
import team3.database.DBSearch.ExcludedSearch;
import team3.database.DBSearch.SmiSearch;
import team3.gui.Gui;
import team3.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import team3.utils.Common;

import javax.swing.*;
import java.sql.*;

public class DBQueries {
    private final Utilities dbUtil = new Utilities();
    private static final int WORD_FREQ_MATCHES = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(DBQueries.class);

    // Заполняем таблицу анализа
    public void selectSqlite(Connection connection) {
        try {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(dbUtil.getSQLQueryFromProp("selectSQLite"));
            preparedStatement.setInt(1, WORD_FREQ_MATCHES);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                String word = rs.getString("TITLE");
                int sum = rs.getInt("SUM");
                Object[] row2 = new Object[]{word, sum};
                Gui.modelForAnalysis.addRow(row2);
            }
            deleteTitles(connection);
            rs.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // запись данных по актуальным источникам из базы в массивы для поиска
    // TODO: Make this the context to using different types of search
    public void selectSources(String pDialog) {
        if (SQLite.isConnectionToSQLite) {
            switch (pDialog) {
                case "smi":
                    DBSearch smiSearch = new SmiSearch();
                    smiSearch.dbSearch();
                    break;
                case "excl":
                    //excluded words
                    DBSearch exclSearch = new ExcludedSearch();
                    exclSearch.dbSearch();
                    break;
                case "active_smi":
                    DBSearch activeSmiSearch = new ActiveSmiSearch();
                    activeSmiSearch.dbSearch();
                    break;
            }
        }
    }

    // вставка нового источника
    public void insertNewSource(Connection connection) {
        try {
            if (connection.isValid(3)) {
                // Диалоговое окно добавления источника новостей в базу данных
                RSSInfoFromUI rssInfoFromUI = dbUtil.getRSSInfoFromUI();
                if (rssInfoFromUI.getResult() == JOptionPane.YES_OPTION) {
                    //запись в БД
                    PreparedStatement preparedStatement =
                            connection.prepareStatement(Main.prop.getProperty("insertNewSource"));
                    preparedStatement.setInt(1, getNextMaxID(connection));
                    preparedStatement.setString(2, rssInfoFromUI.getSourceName().getText());
                    preparedStatement.setString(3, rssInfoFromUI.getRssLink().getText());
                    preparedStatement.executeUpdate();
                    Common.console("status: source added");
                } else {
                    Common.console("status: adding source canceled");
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void insertTitleIn256(String pTitle, Connection connection) {
        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("insertTitle256"));
                preparedStatement.setString(1, pTitle);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException t) {
            t.printStackTrace();
        }
    }


    // сохранение всех заголовков
    public void insertAllTitles(String pTitle, String pDate, Connection connection) {
        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("insertAllTitles"));
                preparedStatement.setString(1, pTitle);
                preparedStatement.setString(2, pDate);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // вставка нового слова для исключения из анализа частоты употребления слов
    public void insertNewExcludedWord(String pWord, Connection connection) {

        try {
            if (connection.isValid(3)) {
                //запись в БД
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("insertExcludeWord"));
                preparedStatement.setString(1, pWord);
                preparedStatement.executeUpdate();

                LOGGER.warn("status: word \"" + pWord + "\" excluded from analysis");
                LOGGER.warn("New word excluded from analysis");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Common.console("status: " + e.getMessage());
        }

    }

    // Delete from news_dual
    public void deleteTitles(Connection connection) {
        try {
            Statement st = connection.createStatement();
            st.executeUpdate(dbUtil.getSQLQueryFromProp("deleteTitles"));
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Delete from titles256
    public void deleteFrom256(Connection connection) {
        try {
            Statement st = connection.createStatement();
            st.executeUpdate(dbUtil.getSQLQueryFromProp("deleteFrom256"));
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // удаление источника
    public void deleteSource(String p_source, Connection connection) {
        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("deleteSource"));
                preparedStatement.setString(1, p_source);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            Common.console("status: " + e.getMessage());
        }

    }

    // удаление дубликатов новостей
    public void deleteDuplicates(Connection connection) {
        try {
            if (connection.isValid(3)) {
                Statement st = connection.createStatement();
                st.executeUpdate(dbUtil.getSQLQueryFromProp("deleteAllDuplicates"));
                st.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // обновление статуса чекбокса is_active для ресурсов SELECT id, source, link FROM rss_list where is_active = 1  ORDER BY id
    public void updateIsActiveStatus(boolean pBoolean, String pSource, Connection connection) {
        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("updateActiveStatus"));
                preparedStatement.setBoolean(1, pBoolean);
                preparedStatement.setString(2, pSource);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    // удаление слова исключенного из поиска
    public void deleteExcluded(String p_source, Connection connection) {
        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("deleteExcluded"));
                preparedStatement.setString(1, p_source);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            Common.console("status: " + e.getMessage());
        }

    }

    // отсеивание заголовков
    public boolean isTitleExists(String pString256, Connection connection) {
        int isExists = 0;

        try {
            if (connection.isValid(3)) {
                PreparedStatement preparedStatement =
                        connection.prepareStatement(dbUtil.getSQLQueryFromProp("titleExists"));
                preparedStatement.setString(1, pString256);
                ResultSet rs = preparedStatement.executeQuery();

                while (rs.next()) {
                    isExists = rs.getInt(1);
                }

                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return isExists == 1;
    }

    // новостей в архиве всего
    public int archiveNewsCount(Connection connection) {
        int countNews = 0;

        try {
            if (connection.isValid(3)) {
                Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(dbUtil.getSQLQueryFromProp("archiveNewsCount"));

                while (rs.next()) {
                    countNews = rs.getInt(1);
                }
                rs.close();
                st.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return countNews;
    }

    private int getNextMaxID(Connection connection) throws SQLException {
        Statement maxIdSt = connection.createStatement();
        ResultSet rs = maxIdSt.executeQuery(dbUtil.getSQLQueryFromProp("maxIdQuery"));
        int maxIdInSource = 0;
        while (rs.next()) {
            maxIdInSource = rs.getInt("ID");
        }
        rs.close();
        maxIdSt.close();
        return maxIdInSource + 1;
    }
}
