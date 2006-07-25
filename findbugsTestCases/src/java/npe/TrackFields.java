package npe;
class TrackFields {
    Object x;
    TrackFields(Object x) {
            this.x = x;
            }

    int f() {
            if (x == null) return x.hashCode();
            return 42;
            }
    int f2() {
    	Object tmp = x;
        if (tmp == null) return x.hashCode();
        return 42;
        }

    int g() {
            if (x != null) return 42;
            return x.hashCode();
            }
    int doNotReport() {
        if (x == null) 
        	x = new Object();
        return x.hashCode();
        }
}
