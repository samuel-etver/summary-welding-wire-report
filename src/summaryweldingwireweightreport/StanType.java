
package summaryweldingwireweightreport;


public enum StanType {
    NONE(null),
    ID("ID"),
    OD("OD");
    
    private StanType(String text) {
        m_Type = text;        
    }
    
    private final String m_Type;
    
    public String getType() {
        return m_Type;
    }
};
