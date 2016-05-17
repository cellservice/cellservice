package de.tu_berlin.snet.cellservice.util.database;

/**
 * Created by Friedhelm Victor on 5/13/16.
 */
public class Migration implements Comparable<Migration>{

    private String sqlStatements;
    private String title;
    private long time;

    public Migration(long time, String title, String sqlStatements) {
        this.time = time;
        this.title = title;
        this.sqlStatements = sqlStatements;
    }

    public long getTime() {
        return this.time;
    }

    public String getTitle() {
        return this.title;
    }

    public String getSqlStatements() {
        return this.sqlStatements;
    }

    public String getSqlStatementsEscaped() {
        return this.sqlStatements.replaceAll("'","''");
    }

    @Override
    public int compareTo(Migration another) {
        if(getTime() < another.getTime()) {
            return -1;
        } else if(getTime() > another.getTime()) {
            return 1;
        } else {
            return 0;
        }
    }
}