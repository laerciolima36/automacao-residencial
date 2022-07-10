package com.example.automacaoresidencial;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

@SuppressLint("HandlerLeak")
public class principal extends AppCompatActivity {

    static String IP = "";
    static String TipoBotaoPortao = "ligadesliga";
    static int retorno;
    int pass;
    int TempoPulso = 1000;

    Button botao1, botao2, btconfiguracao;
    TextView btfalar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        getSupportActionBar().hide();

        carregaViews();



        //*******************************************************************
        btfalar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSpeechInput();
            }
        });

        //*******************************************************************
        botao1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnviaDados(12);
            }
        });

        //*******************************************************************
        botao2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnviaDados(15);
            }
        });

        //*******************************************************************
        btconfiguracao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChamaConfig();
            }
        });

    }

    @Override
    public void onStart() {
        try{
            IP = LerArquivo("ip.txt");
            TipoBotaoPortao = LerArquivo("tipoBotao.txt");

            TempoPulso = Integer.parseInt(LerArquivo("tempo.txt"));

            pass = Integer.parseInt(LerArquivo("senha.txt"));

            getSpeechInput();
        }catch (Exception e){
            System.out.println("erro no onstart" + e);
            ChamaConfig();
        }
        super.onStart();
    }

    //TODO LER UM ARQUIVO ARMAZENADO NA MEMORIA INTERNA DO CELULAR
    public String LerArquivo(String File) {
        String text = "";

        try {
            FileInputStream fileInputStream = openFileInput(File);
            int size = fileInputStream.available();
            byte[] buffer = new byte[size];
            fileInputStream.read(buffer);
            fileInputStream.close();
            text = new String(buffer);
            System.out.println("Carregado " + File);

        } catch (Exception e) {
            System.out.println("Erro no Metodo Ler Arquivo: " + e);
            e.printStackTrace();
            ChamaConfig();
        }
        return text;
    }



    public void carregaViews(){
        botao1 = findViewById(R.id.botao1);
        botao2 = findViewById(R.id.botao2);
        btconfiguracao = findViewById(R.id.btconfiguracao);
        btfalar = findViewById(R.id.btfalar);
    }

    public void EnviaDados(int PINO) { //PINO = endereço modbus HoldingRegisters
        Escrita enviaModbus = new Escrita();
        enviaModbus.execute(PINO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Una vez que se ha realizado una actividad regresa un "resultado"...
        switch (requestCode) {

            case 10:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(this, result.get(0), Toast.LENGTH_LONG).show();
                    String acao = result.get(0);

                    if(acao.equals("abrir portão") || acao.equals("Abrir portão") || acao.equals("fechar portão") || acao.equals("Fechar portão")){
                        EnviaDados(15);
                    }

                    if(acao.equals("ligar luz") || acao.equals("Ligar luz") || acao.equals("Apagar a luz") || acao.equals("apaga a luz") || acao.equals("Ligar a luz")){
                        EnviaDados(12);
                    }

                }
        }
    }

    public void getSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 10);
        } else {
            Toast.makeText(this, "Dispositivo não Suportado!", Toast.LENGTH_SHORT).show();
        }
    }

    //TODO THREAD PRINCIPAL MODBUS
    public class Escrita extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            Integer estado = integers[0];

            try {
                TcpParameters tcpParameters = new TcpParameters();

                //tcp parameters have already set by default as in example
                tcpParameters.setHost(InetAddress.getByName(IP));
                tcpParameters.setKeepAlive(true);
                tcpParameters.setPort(Modbus.TCP_PORT);

                //if you would like to set connection parameters separately,
                // you should use another method: createModbusMasterTCP(String host, int port, boolean keepAlive);
                ModbusMaster m = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
                Modbus.setAutoIncrementTransactionId(true);
                m.setResponseTimeout(3000);

                try {
                    // since 1.2.8
                    if (!m.isConnected()) {
                        m.connect();
                    }

                    //ler a senha da placa
                    int[] senha = m.readHoldingRegisters(1, 10, 1);
                    System.out.println("Senha ESP: " + senha[0]);
                    System.out.println("Senha APP: " + pass);

                    int[] status;

                    if (TipoBotaoPortao.equals("ligadesliga") && pass == senha[0]) {
                        status = m.readHoldingRegisters(1, estado, 1);
                        retorno = status[0];

                        if (status[0] == 1) {
                                m.writeSingleRegister(1, estado, 0);
                            } else {
                                m.writeSingleRegister(1, estado, 1);
                            }
                    }

                    if (TipoBotaoPortao.equals("pulso") && pass == senha[0]) {
                        status = m.readHoldingRegisters(1, estado, 1);
                        retorno = status[0];

                        if (status[0] == 1) {
                            m.writeSingleRegister(1, estado, 0);
                        } else {
                            m.writeSingleRegister(1, estado, 1);
                        }

                        Thread.sleep(TempoPulso);

                        status = m.readHoldingRegisters(1, estado, 1);
                        retorno = status[0];

                        if (status[0] == 1) {
                            m.writeSingleRegister(1, estado, 0);
                        } else {
                            m.writeSingleRegister(1, estado, 1);
                        }

                    }


                } catch (ModbusProtocolException e) {
                    e.printStackTrace();
                } catch (ModbusNumberException e) {
                    e.printStackTrace();
                } catch (ModbusIOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        m.disconnect();
                    } catch (ModbusIOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }



    //TODO ACTIVITY DE CONFIGURAÇÕES
    public void ChamaConfig() {
        Intent config = new Intent(this, Config.class);
        startActivity(config);
    }
}
