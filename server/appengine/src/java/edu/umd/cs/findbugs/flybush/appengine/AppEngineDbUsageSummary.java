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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppEngineDbUsageSummary that = (AppEngineDbUsageSummary) o;

        if (value != that.value) return false;
        if (category != null ? !category.equals(that.category) : that.category != null) return false;
        if (categoryKey != null ? !categoryKey.equals(that.categoryKey) : that.categoryKey != null) return false;
        if (categorySubkey != null ? !categorySubkey.equals(that.categorySubkey) : that.categorySubkey != null)
            return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (lastUpdated != null ? !lastUpdated.equals(that.lastUpdated) : that.lastUpdated != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (categoryKey != null ? categoryKey.hashCode() : 0);
        result = 31 * result + (categorySubkey != null ? categorySubkey.hashCode() : 0);
        result = 31 * result + value;
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(DbUsageSummary o) {
        if (o.getDate() == null) 
            return date == null ? -1 : 1;
        if (date == null) 
            return -1;
        int compare = date.compareTo(o.getDate());
        if (compare != 0)
            return compare;
        int a = System.identityHashCode(this);
        int b = System.identityHashCode(o);
        return a < b ? -1 : (a > b ? 1 : 0);
    }
}
