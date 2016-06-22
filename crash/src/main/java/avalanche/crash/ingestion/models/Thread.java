package avalanche.crash.ingestion.models;

import java.util.List;


public class Thread {

    private Integer id = null;
    private List<ThreadFrame> frames = null;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<ThreadFrame> getFrames() {
        return frames;
    }

    public void setFrames(List<ThreadFrame> frames) {
        this.frames = frames;
    }
}
