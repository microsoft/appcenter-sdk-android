package avalanche.crash.ingestion.models;


public class ThreadFrame {

    private String address = null;
    private String symbol = null;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ThreadFrame threadFrame = (ThreadFrame) o;
        return (address == null ? threadFrame.address == null : address.equals(threadFrame.address)) &&
                (symbol == null ? threadFrame.symbol == null : symbol.equals(threadFrame.symbol));
    }

}
