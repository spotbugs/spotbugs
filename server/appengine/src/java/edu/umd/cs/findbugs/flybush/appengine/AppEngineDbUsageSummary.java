package edu.umd.cs.findbugs.flybush.appengine;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import edu.umd.cs.findbugs.flybush.DbUsageEntry;
import edu.umd.cs.findbugs.flybush.DbUsageSummary;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbUsageSummary implements DbUsageSummary {

    @SuppressWarnings({ "UnusedDeclaration" })
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private Date date;
    @Persistent
    private String category;
    @Persistent
    private String categoryKey;
    @Persistent
    private String categorySubkey;
    @Persistent
    private int value;
    @Persistent
    private Date lastUpdated;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public String getCategorySubkey() {
        return categorySubkey;
    }

    public void setCategorySubkey(String categorySubkey) {
        this.categorySubkey = categorySubkey;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public DbUsageSummary copy() {
        AppEngineDbUsageSummary copy = new AppEngineDbUsageSummary();
        copy.setDate(date);
        copy.setLastUpdated(lastUpdated);
        copy.setCategory(category);
        copy.setCategoryKey(categoryKey);
        copy.setCategorySubkey(categorySubkey);
        copy.setValue(value);
        return copy;
    }

    @Override
    public String toString() {
        return "AppEngineDbUsageSummary{" +
                "date=" + date +
                ", category='" + category + '\'' +
                ", categoryKey='" + categoryKey + '\'' +
                ", categorySubkey='" + categorySubkey + '\'' +
                ", value=" + value +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
