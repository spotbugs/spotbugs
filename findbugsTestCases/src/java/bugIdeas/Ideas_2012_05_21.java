package bugIdeas;

public class Ideas_2012_05_21 {
    
    String firstName;

    public Ideas_2012_05_21(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public int hashCode() {
        return firstName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ideas_2012_05_21))
            return false;
        Ideas_2012_05_21 guest = (Ideas_2012_05_21) obj;
        return firstName == guest.firstName || firstName != null && firstName.equals(guest.firstName);
    }
    
    

}
