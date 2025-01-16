package si.um.feri.kellner.Pos;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

public class PosServer {
    Server server;
    HashSet<Character> loggedIn = new HashSet();

    public PosServer () throws IOException {
        server = new Server() {
            protected Connection newConnection () {
                // By providing our own connection implementation, we can store per
                // connection state without a connection ID to state look up.
                return new CharacterConnection();
            }
        };

        // For consistency, the classes to be sent over the network are
        // registered by the same method for both the client and server.
        Net.register(server);

        server.addListener(new Listener() {
            public void received (Connection c, Object object) {
                // We know all connections for this server are actually CharacterConnections.
                CharacterConnection connection = (CharacterConnection)c;
                Character character = connection.character;

                if (object instanceof Net.Login) {
                    // Ignore if already logged in.
                    if (character != null) return;

                    // Reject if the name is invalid.
                    String name = ((Net.Login)object).name;
                    if (!isValid(name)) {
                        c.close();
                        return;
                    }

                    // Reject if already logged in.
                    for (Character other : loggedIn) {
                        if (other.name.equals(name)) {
                            c.close();
                            return;
                        }
                    }

                    character = loadCharacter(name);

                    // Reject if couldn't load character.
                    if (character == null) {
                        c.sendTCP(new Net.RegistrationRequired());
                        return;
                    }

                    loggedIn(connection, character);
                    return;
                }

                if (object instanceof Net.Register) {
                    // Ignore if already logged in.
                    if (character != null) return;

                    Net.Register register = (Net.Register)object;

                    // Reject if the login is invalid.
                    if (!isValid(register.name)) {
                        c.close();
                        return;
                    }
                    if (!isValid(register.otherStuff)) {
                        c.close();
                        return;
                    }

                    // Reject if character alread exists.
                    if (loadCharacter(register.name) != null) {
                        c.close();
                        return;
                    }

                    character = new Character();
                    character.name = register.name;
                    character.otherStuff = register.otherStuff;
                    character.x = 0;
                    character.y = 0;
                    if (!saveCharacter(character)) {
                        c.close();
                        return;
                    }

                    loggedIn(connection, character);
                    return;
                }

                if (object instanceof Net.MoveCharacter) {
                    // Ignore if not logged in.
                    if (character == null) return;

                    Net.MoveCharacter msg = (Net.MoveCharacter)object;

                    // Ignore if invalid move.
                    if (Math.abs(msg.x) != 1 && Math.abs(msg.y) != 1) return;

                    character.x += msg.x;
                    character.y += msg.y;
                    if (!saveCharacter(character)) {
                        connection.close();
                        return;
                    }

                    Net.UpdateCharacter update = new Net.UpdateCharacter();
                    update.id = character.id;
                    update.x = character.x;
                    update.y = character.y;
                    server.sendToAllTCP(update);
                    return;
                }
            }

            private boolean isValid (String value) {
                if (value == null) return false;
                value = value.trim();
                if (value.length() == 0) return false;
                return true;
            }

            public void disconnected (Connection c) {
                CharacterConnection connection = (CharacterConnection)c;
                if (connection.character != null) {
                    loggedIn.remove(connection.character);

                    Net.RemoveCharacter removeCharacter = new Net.RemoveCharacter();
                    removeCharacter.id = connection.character.id;
                    server.sendToAllTCP(removeCharacter);
                }
            }
        });
        server.bind(Net.port);
        server.start();
    }

    void loggedIn (CharacterConnection c, Character character) {
        c.character = character;

        // Add existing characters to new logged in connection.
        for (Character other : loggedIn) {
            Net.AddCharacter addCharacter = new Net.AddCharacter();
            addCharacter.character = other;
            c.sendTCP(addCharacter);
        }

        loggedIn.add(character);

        // Add logged in character to all connections.
        Net.AddCharacter addCharacter = new Net.AddCharacter();
        addCharacter.character = character;
        server.sendToAllTCP(addCharacter);
    }

    boolean saveCharacter (Character character) {
        File file = new File("characters", character.name.toLowerCase());
        file.getParentFile().mkdirs();

        if (character.id == 0) {
            String[] children = file.getParentFile().list();
            if (children == null) return false;
            character.id = children.length + 1;
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(file));
            output.writeInt(character.id);
            output.writeUTF(character.otherStuff);
            output.writeInt(character.x);
            output.writeInt(character.y);
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {
            }
        }
    }

    Character loadCharacter (String name) {
        File file = new File("characters", name.toLowerCase());
        if (!file.exists()) return null;
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(file));
            Character character = new Character();
            character.id = input.readInt();
            character.name = name;
            character.otherStuff = input.readUTF();
            character.x = input.readInt();
            character.y = input.readInt();
            input.close();
            return character;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (input != null) input.close();
            } catch (IOException ignored) {
            }
        }
    }

    // This holds per connection state.
    static class CharacterConnection extends Connection {
        public Character character;
    }

    public static void main (String[] args) throws IOException {
        Log.set(Log.LEVEL_DEBUG);
        new PosServer();
    }
}
