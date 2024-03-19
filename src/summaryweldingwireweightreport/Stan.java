package summaryweldingwireweightreport;

public class Stan {    
    private Double m_WireWeigth;
    private StanDiameter m_StanDiameter;
    private StanType m_StanType;
    private Integer m_StanNumber;
    
    
    public double getWireWeight() {
        return m_WireWeigth;
    }    
    
    
    public StanDiameter getStanDiameter() {
        return m_StanDiameter;
    }
    
    
    public StanType getStanType() {
        return m_StanType;
    }

    
    public Integer getStanNumber() {
        return m_StanNumber;
    }
    
    
    public static class Builder {
        public StanDiameter stanDiameter;
        public StanType stanType;
        public Double wireWeight;
        public Integer stanNumber;
        
        
        public Stan build() {
            if (wireWeight == null
                || stanNumber == null
                || stanType == null
                || stanDiameter == null) {                
                return null;
            }
            
            final Stan stan = new Stan();
            stan.m_WireWeigth = wireWeight;
            stan.m_StanType = stanType;
            stan.m_StanDiameter = stanDiameter;
            stan.m_StanNumber = stanNumber;
            return stan;
        }
    }
}
