package javaaudio.cliente;

// Soporte archivo proto
import com.proto.audio.AudioServiceGrpc;
import com.proto.audio.Audio.DownloadFileRequest;
import com.proto.audio.AudioServiceGrpc.AudioServiceBlockingStub;

// Soporte para enviar stream gRPC
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

// Siporte para archivos
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

// Player WAV
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

// Player MP3
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;


public class Cliente {
    public static void main(String[] args) {
        // Establece el servidor gRPC
        String host = "localhost";

        // Puerto
        int puerto = 9090;

        // Guarda el nombre del archivo a solicitar
        String nombre;

        // Crea el canal de comunicación
        ManagedChannel ch = ManagedChannelBuilder.forAddress(host, puerto).usePlaintext().build();
    
        // Recibe el archivo WAV y lo reproduce mientras llega
        //nombre = "sample.wav";
        //streamWav(ch, nombre, 44100F);

        // nombre = "anyma.wav";
        // streamWav(ch, nombre, 48000F);

        // Solicita archivo MP3 al servidor
        nombre = "tiesto.mp3";
        ByteArrayInputStream streamMP3 = downloadFile(ch, nombre);

        // Reproduce MP3
        playMp3(streamMP3, nombre);
        try {
            streamMP3.close();
        } catch (IOException e) {
        }

        // Solicita archivo WAV al servidor
        /*nombre = "sample.wav";
        ByteArrayInputStream streamWAV = downloadFile(ch, nombre);

        // Reproduce
        playWav(streamWAV, nombre);
        try {
            streamWAV.close();
        } catch (IOException e) {
        }*/


        // Terminamos la comunicación
        System.out.println("Apagando...");
        ch.shutdown();
    }

    // Descarga y reproduce al mismo tiempo
    public static void streamWav(ManagedChannel ch, String nombre, float sampleRate) {
        try {
            // AudioFormat (float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian)
            AudioFormat newFormat = new AudioFormat(sampleRate, 16, 2, true, false);
            SourceDataLine sourceDataLine = AudioSystem.getSourceDataLine(newFormat);
            sourceDataLine.open(newFormat);
            sourceDataLine.start();

            // Obtenemos la referencia la stub
            AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);

            // Construimos la petición enviando un parametro
            DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

            // Establecemos una longitud de chunk
            int bufferSize = 1024;
            System.out.println("Reproduciendo el archivo: " + nombre);

            stub.downloadAudio(peticion).forEachRemaining(respuesta -> {
                try {
                   sourceDataLine.write(respuesta.getData().toByteArray(), 0, bufferSize);
                   System.out.println("."); 
                } catch (Exception e) {
                }
            });

            System.out.println("\nRecepción de datos correcta");
            System.out.println("Reproducción terminada. \n\n");

            // Cerramos la linea
            sourceDataLine.drain();
            sourceDataLine.close();

        } catch (LineUnavailableException e) {
            System.out.println(e.getMessage());
        }
    }

    // Descarga el archivo (stream)
    public static ByteArrayInputStream downloadFile(ManagedChannel ch, String nombre) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // Obtenemos la referencia al stub
        AudioServiceBlockingStub stub = AudioServiceGrpc.newBlockingStub(ch);

        // Construimos la petición enviando un parametro
        DownloadFileRequest peticion = DownloadFileRequest.newBuilder().setNombre(nombre).build();

        System.out.println("Recibiendo el archivo: " + nombre);

        // Usando el stub, realizamos la llamada streaming RPC
        stub.downloadAudio(peticion).forEachRemaining(repsuesta -> {
            try {
                stream.write(repsuesta.getData().toByteArray());
                System.out.println(".");
            } catch (Exception e) {
                System.out.println("No se pudo obtener el archivo de audio.");
            }
        });

        // Se recibieron todos los datos
        System.out.println("\nRecepción de datos correcta.\n\n");

        //Convierte el output stream a uno de entrada para su reproducción
        return new ByteArrayInputStream(stream.toByteArray());
    }

    // Reproduce un archivo WAV descargado
    public static void playWav(ByteArrayInputStream inStream, String nombre) {
        try {
            AudioInputStream audioInput = AudioSystem.getAudioInputStream(inStream);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n\n");
            clip.start();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            clip.stop();

        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    // Reproduce un archivo MP3 descargado
    public static void playMp3(ByteArrayInputStream inStream, String nombre) {
        try {
            System.out.println("Reproduciendo el archivo: " + nombre + "...\n");
            Player player = new Player(inStream);
            player.play();
        } catch (JavaLayerException e) {
            e.printStackTrace();
        }
    }
}
