package interfaces;

import java.io.Serializable;

/**
 * Created by jeroen on 11/22/17.
 * Implementation for message that gets passed around in DS.
 */
public class Message implements Serializable {
    public int id;
    private static final long serialVersionUID = 20120731125400L;

    public Message(int id) {
        this.id = id;
    }
}
