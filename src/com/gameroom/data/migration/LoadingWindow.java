package com.gameroom.data.migration;

import com.gameroom.ui.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * This is a Swing window imitating GameRoom's look and feel to display a loading bar. It does not use JavaFX to allow to
 * start this window before having the application start. This is used for example for the migration to the {@link com.gameroom.data.io.DataBase}
 * using the {@link OldGameEntry} for example.
 *
 * @author LM. Garret (admin@gameroom.me)
 * @date 17/07/2017.
 */
public class LoadingWindow {
    private static LoadingWindow INSTANCE;

    private Color flatterRed = new Color(0.9294f, 0.2666f, 0.1451f);
    private Color flatterDark = new Color(0.1843f, 0.2039f, 0.2235f);
    private JProgressBar progressBar;
    private JButton quitButton;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JFrame loadingFrame;
    private Font font;
    private Font fontSmall;

    private LoadingWindow() {
        font = new Font("Helvetica Neue", Font.PLAIN, 18);
        fontSmall = new Font("Helvetica Neue", Font.PLAIN, 14);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setBackground(flatterDark);
        progressBar.setForeground(flatterRed);
        progressBar.setFont(fontSmall);
        progressBar.setUI(new BasicProgressBarUI() {
            protected Color getSelectionBackground() {
                return Color.white;
            }

            protected Color getSelectionForeground() {
                return Color.white;
            }
        });

        quitButton = new JButton("OK");
        /*quitButton.setBackground(flatterDark);
        quitButton.setForeground(flatterRed);*/
        quitButton.setFont(fontSmall);
        quitButton.setVisible(false);
        quitButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadingFrame.dispose();
                javafx.application.Platform.exit();
            }
        });

        titleLabel = new JLabel(Main.getString("applying_update_title"));
        titleLabel.setForeground(Color.white);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(font);

        statusLabel = new JLabel("applying_update_title");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        statusLabel.setForeground(Color.white);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setFont(fontSmall);

        loadingFrame = new JFrame();
    }

    /**
     * Finishes to build the dialog and displays it
     */
    public void show() {
        ImageIcon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(ClassLoader.getSystemResource("ui/icon/icon64.png")));
            loadingFrame.setIconImage(icon.getImage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadingFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        loadingFrame.setUndecorated(true);
        loadingFrame.setBackground(flatterDark);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        loadingFrame.setLocation((int) ((screenSize.getWidth() - 600) / 2), (int) ((screenSize.getHeight() - 0) / 2));

        JPanel textAndBarPanel = new JPanel();
        textAndBarPanel.setLayout(new BoxLayout(textAndBarPanel, BoxLayout.PAGE_AXIS));
        textAndBarPanel.add(titleLabel);
        textAndBarPanel.add(statusLabel);
        textAndBarPanel.add(progressBar);
        textAndBarPanel.add(quitButton);
        textAndBarPanel.setBackground(flatterDark);

        JLabel iconLabel = new JLabel(icon);
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(iconLabel, BorderLayout.LINE_START);
        contentPanel.add(textAndBarPanel, BorderLayout.CENTER);
        contentPanel.setBackground(flatterDark);

        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loadingFrame.getContentPane().add(contentPanel);
        loadingFrame.getRootPane().setBorder(BorderFactory.createLineBorder(flatterRed));
        loadingFrame.pack();
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);
    }

    /**
     * Disposes of the window
     */
    public void dispose() {
        loadingFrame.dispose();
    }

    public static LoadingWindow getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LoadingWindow();
        }
        return INSTANCE;
    }

    /**
     * Changes the progress associated to this LoadingWindow
     *
     * @param value value of the progress, from 0 to {@link JProgressBar#getMaximum()}; or -1 for an error, which displays
     *              a quit button
     * @param text  the text associated to those changes
     */
    public void setProgress(int value, String text) {
        if(value <0){
            progressBar.setVisible(false);
            quitButton.setVisible(true);
        }else {
            progressBar.setValue(value);
        }
        statusLabel.setText(text);
    }

    /**
     * Sets the maximum progress of the {@link #progressBar}
     *
     * @param value the maximum value
     */
    public void setMaximumProgress(int value) {
        progressBar.setMaximum(value);
    }


}
