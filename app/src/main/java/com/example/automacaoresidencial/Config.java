package com.example.automacaoresidencial;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Config extends AppCompatActivity {

    Button btsalvar, btAuto;
    TextView editIP, editTempo, editSenha;
    RadioButton checkLigaDesliga, checkPulso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        carregaviews();
        carregaconfig();
        //buscaAutomaticaIP();

        //salvarArquivo("tempo.txt", "1000");

        checkLigaDesliga.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTempo.setVisibility(View.GONE);
            }
        });

        checkPulso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editTempo.setVisibility(View.VISIBLE);
            }
        });

        btsalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvaConfig();
            }
        });

        btAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buscaAutomaticaIP();
            }
        });
    }

    private void buscaAutomaticaIP() {
        BuscaPlaca buscaPlaca = new BuscaPlaca();
        buscaPlaca.execute(getIpWifi());
    }

    private void salvaConfig() {
        if (editIP.getText().equals("")) {
            Toast.makeText(Config.this, "Campo IP ou DDNS em Branco!", Toast.LENGTH_LONG).show();
        } else {
            salvarArquivo("ip.txt", editIP.getText().toString().replaceAll(" ", ""));
        }

        if (editSenha.getText().equals("")) {
            Toast.makeText(Config.this, "Senha em Branco!", Toast.LENGTH_LONG).show();
        } else {
            salvarArquivo("senha.txt", editSenha.getText().toString().replaceAll(" ", ""));
        }

        if (editTempo.getText().equals("") || editTempo.getVisibility() == View.GONE) {
            salvarArquivo("tempo.txt", "500");
            Toast.makeText(Config.this, "No Time!", Toast.LENGTH_LONG).show();
        } else {
            salvarArquivo("tempo.txt", editTempo.getText().toString().replaceAll(" ", ""));
        }

        if (checkLigaDesliga.isChecked()) {
            salvarArquivo("tipoBotao.txt", "ligadesliga");
        }

        if (checkPulso.isChecked()) {
            salvarArquivo("tipoBotao.txt", "pulso");
        }

        Intent Principal = new Intent(Config.this, principal.class);
        startActivity(Principal);
    }

    private void carregaconfig() {
        editIP.setText(LerArquivo("ip.txt"));
        editTempo.setText(LerArquivo("tempo.txt"));
        editSenha.setText(LerArquivo("senha.txt"));

        if (LerArquivo("tipoBotao.txt").equals("pulso")) {
            checkPulso.setChecked(true);
            editTempo.setVisibility(View.VISIBLE);
        } else {
            checkLigaDesliga.setChecked(true);
            editTempo.setVisibility(View.GONE);
        }
    }

    public void carregaviews(){
        editIP = findViewById(R.id.editIP);
        editTempo = findViewById(R.id.editTempo);
        editSenha = findViewById(R.id.editSenha);

        btAuto = findViewById(R.id.btAuto);
        btsalvar = findViewById(R.id.btsalvar);
        checkLigaDesliga = findViewById(R.id.checkLigaDesliga);
        checkPulso = findViewById(R.id.checkPulso);
    }

    public void salvarArquivo(String file, String text) {
        try {
            FileOutputStream outputStream = openFileOutput(file, Context.MODE_PRIVATE);
            outputStream.write(text.getBytes());
            outputStream.close();
            System.out.println("Salvo " + file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Erro ao Salvar o Arquivo!", Toast.LENGTH_SHORT).show();
        }

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
            e.printStackTrace();
        }

        return text;
    }

    public Integer[] getIpWifi(){
        Integer[] ip = new Integer[4];

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        String ipstring =  Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());

        String[] ipSeparado = ipstring.split("[.]");

        ip[0] = Integer.parseInt(ipSeparado[0]);
        ip[1] = Integer.parseInt(ipSeparado[1]);
        ip[2] = Integer.parseInt(ipSeparado[2]);
        ip[3] = Integer.parseInt(ipSeparado[3]);

        return ip;
    }


    public class BuscaPlaca extends AsyncTask<Integer, Integer, String> {

        private boolean portaEstaAberta(String ip, int porta, int timeout) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, porta), timeout);
                socket.close();
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        protected String doInBackground(Integer... integers) {
            String ipPlaca = "";

            try {

                byte[] ipWifi = new byte[]{(byte) integers[0].intValue(), (byte) integers[1].intValue(), (byte) integers[2].intValue(), (byte) integers[3].intValue()};

                InetAddress localhost = InetAddress.getByAddress(ipWifi);

                byte[] ip = localhost.getAddress();
                System.out.println("Endereço IP do Wifi: " + localhost.toString());

                for (int i = 1; i <= 254; i++) {
                    ip[3] = (byte) i;
                    InetAddress address = InetAddress.getByAddress(ip);

                    if (address.isReachable(800)) {
                        System.out.println(address + " maquina esta ligada e pode ser pingada");
                    } else if (!address.getHostAddress().equals(address.getHostName())) {
                        System.out.println(address + " maquina reconhecida por um DNSLookup");
                    } else {
                        System.out.println(address + " o endereço de host e o nome do host são iguais, o host name não pode ser resolvido.");
                    }

                    if (portaEstaAberta(address.getHostAddress(), 502, 800)) {
                        ipPlaca = address.getHostAddress();
                        System.out.println("Ip Placa****>: " + ipPlaca);
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro no metodo buscaIP: " + e.toString());
            }

            return ipPlaca;
        }

        @Override
        protected void onPostExecute(String ipPlaca) {
            super.onPostExecute(ipPlaca);

            editIP.setText(ipPlaca);
            salvarArquivo("ip.txt", ipPlaca);
        }
    }
}
