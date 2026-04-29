package chinook;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class ChinookApp extends JFrame {

    private static final Color BG       = new Color(18, 18, 30);
    private static final Color PANEL_BG = new Color(28, 28, 46);
    private static final Color ACCENT   = new Color(99, 102, 241);
    private static final Color ACCENT2  = new Color(139, 92, 246);
    private static final Color SUCCESS  = new Color(52, 211, 153);
    private static final Color DANGER   = new Color(239, 68, 68);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color SUBTEXT  = new Color(148, 163, 184);
    private static final Color ROW_ALT  = new Color(38, 38, 60);
    private static final Color BORDER_C = new Color(55, 55, 80);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BODY  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    private Connection conn;
    private JLabel statusBar;

    public ChinookApp() {
        super("Chinook Music Store - COS221 PA4");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        if (!connectDB()) {
            JOptionPane.showMessageDialog(null,
                "Could not connect to the database.\nCheck your CHINOOK_DB_* environment variables.",
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        buildUI();
        setVisible(true);
    }

    private boolean connectDB() {
        String proto    = env("CHINOOK_DB_PROTO",    "jdbc:mysql");
        String host     = env("CHINOOK_DB_HOST",     "localhost");
        String port     = env("CHINOOK_DB_PORT",     "3306");
        String dbName   = env("CHINOOK_DB_NAME",     "Chinook");
        String user     = env("CHINOOK_DB_USERNAME", "root");
        String password = env("CHINOOK_DB_PASSWORD", "");
        String url = proto + "://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            conn = DriverManager.getConnection(url, user, password);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_BG);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));
        JLabel title = new JLabel("  Chinook Music Store", SwingConstants.LEFT);
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT);
        title.setBorder(new EmptyBorder(14, 20, 14, 0));
        header.add(title, BorderLayout.WEST);
        JLabel sub = new JLabel("COS221 PA4  ", SwingConstants.RIGHT);
        sub.setFont(FONT_SMALL);
        sub.setForeground(SUBTEXT);
        header.add(sub, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG);
        tabs.setForeground(TEXT);
        tabs.setFont(FONT_LABEL);
        tabs.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabs.addTab("Employees",       buildEmployeesTab());
        tabs.addTab("Tracks",          buildTracksTab());
        tabs.addTab("Report",          buildReportTab());
        tabs.addTab("Notifications",   buildNotificationsTab());
        tabs.addTab("Recommendations", buildRecommendationsTab());

        add(tabs, BorderLayout.CENTER);

        statusBar = new JLabel("  Connected to database");
        statusBar.setFont(FONT_SMALL);
        statusBar.setForeground(SUCCESS);
        statusBar.setBackground(PANEL_BG);
        statusBar.setOpaque(true);
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        statusBar.setPreferredSize(new Dimension(0, 28));
        add(statusBar, BorderLayout.SOUTH);

        tabs.addChangeListener(e -> { if (tabs.getSelectedIndex() == 2) refreshReport(); });

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                loadEmployees("");
                loadTracks("");
                loadCustomers("");
                loadInactive("");
                if (custCombo.getItemCount() > 0) loadInsights();
            }
        });
    }

    // -- TAB 1: EMPLOYEES ──────────────────────────────────────────────────────
    private DefaultTableModel empModel;
    private JTextField empSearch;

    private JPanel buildEmployeesTab() {
        JPanel p = darkPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        empSearch = styledField(26);
        JButton btn = accentButton("Search");
        JButton clr = ghostButton("Clear");
        top.add(styledLabel("Filter by name or city:")); top.add(empSearch); top.add(btn); top.add(clr);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"First Name","Last Name","Title","City","Country","Phone","Supervisor"};
        empModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} };
        p.add(styledScroll(styledTable(empModel)), BorderLayout.CENTER);
        btn.addActionListener(e -> loadEmployees(empSearch.getText().trim()));
        clr.addActionListener(e -> { empSearch.setText(""); loadEmployees(""); });
        empSearch.addActionListener(e -> loadEmployees(empSearch.getText().trim()));
        return p;
    }

    private void loadEmployees(String filter) {
        String sql = "SELECT e.FirstName, e.LastName, e.Title, e.City, e.Country, e.Phone, " +
                     "COALESCE(CONCAT(m.FirstName,' ',m.LastName),'--') AS Supervisor " +
                     "FROM Employee e LEFT JOIN Employee m ON e.ReportsTo = m.EmployeeId " +
                     "WHERE (LOWER(CONCAT(e.FirstName,' ',e.LastName)) LIKE ? OR LOWER(e.City) LIKE ?) " +
                     "ORDER BY e.LastName, e.FirstName";
        String like = "%" + filter.toLowerCase() + "%";
        populateTable(empModel, sql, like, like);
        setStatus("Employees loaded" + (filter.isEmpty() ? "" : " - filter: " + filter));
    }

    // ── TAB 2: TRACKS ─────────────────────────────────────────────────────────
    private DefaultTableModel trackModel;
    private JTextField trackSearch;

    private JPanel buildTracksTab() {
        JPanel p = darkPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        trackSearch = styledField(26);
        JButton searchBtn = accentButton("Search");
        JButton addBtn    = accentButton("+ Add Track");
        top.add(styledLabel("Search tracks:")); top.add(trackSearch); top.add(searchBtn); top.add(addBtn);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"TrackId","Name","Album","Artist","Genre","MediaType","Milliseconds","Price"};
        trackModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} };
        p.add(styledScroll(styledTable(trackModel)), BorderLayout.CENTER);
        searchBtn.addActionListener(e -> loadTracks(trackSearch.getText().trim()));
        trackSearch.addActionListener(e -> loadTracks(trackSearch.getText().trim()));
        addBtn.addActionListener(e -> showAddTrackDialog());
        return p;
    }

    private void loadTracks(String filter) {
        String sql = "SELECT t.TrackId, t.Name, al.Title AS Album, ar.Name AS Artist, " +
                     "g.Name AS Genre, mt.Name AS MediaType, t.Milliseconds, t.UnitPrice " +
                     "FROM Track t " +
                     "LEFT JOIN Album al ON t.AlbumId = al.AlbumId " +
                     "LEFT JOIN Artist ar ON al.ArtistId = ar.ArtistId " +
                     "LEFT JOIN Genre g ON t.GenreId = g.GenreId " +
                     "LEFT JOIN MediaType mt ON t.MediaTypeId = mt.MediaTypeId " +
                     "WHERE LOWER(t.Name) LIKE ? OR LOWER(ar.Name) LIKE ? OR LOWER(al.Title) LIKE ? " +
                     "ORDER BY t.TrackId DESC LIMIT 500";
        String like = "%" + filter.toLowerCase() + "%";
        populateTable(trackModel, sql, like, like, like);
        setStatus("Tracks loaded" + (filter.isEmpty() ? " (first 500)" : " - filter: " + filter));
    }

    private void showAddTrackDialog() {
        JDialog dlg = new JDialog(this, "Add New Track", true);
        dlg.setSize(480, 420);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(PANEL_BG);
        dlg.setLayout(new BorderLayout(12, 12));
        JPanel form = darkPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(new EmptyBorder(20, 24, 10, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameF  = styledField(20);
        JTextField msF    = styledField(20);
        JTextField priceF = styledField(20);
        JComboBox albumCB = new JComboBox(); albumCB.setBackground(BG); albumCB.setForeground(TEXT);
        JComboBox genreCB = new JComboBox(); genreCB.setBackground(BG); genreCB.setForeground(TEXT);
        JComboBox mediaCB = new JComboBox(); mediaCB.setBackground(BG); mediaCB.setForeground(TEXT);

        loadCombo(albumCB, "SELECT AlbumId, Title FROM Album ORDER BY Title");
        loadCombo(genreCB, "SELECT GenreId, Name FROM Genre ORDER BY Name");
        loadCombo(mediaCB, "SELECT MediaTypeId, Name FROM MediaType ORDER BY Name");

        ListCellRenderer r = new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList l, Object v, int i, boolean s, boolean f) {
                String[] arr = (String[]) v;
                JLabel lb = new JLabel(arr == null ? "" : arr[1]);
                lb.setForeground(TEXT); lb.setBackground(s ? ACCENT : BG); lb.setOpaque(true);
                return lb;
            }
        };
        albumCB.setRenderer(r); genreCB.setRenderer(r); mediaCB.setRenderer(r);

        String[] labels = {"Track Name:","Album:","Genre:","Media Type:","Duration (ms):","Price ($):"};
        Component[] fields = {nameF, albumCB, genreCB, mediaCB, msF, priceF};
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx=0; gbc.gridy=i; gbc.weightx=0; form.add(styledLabel(labels[i]), gbc);
            gbc.gridx=1; gbc.weightx=1; form.add(fields[i], gbc);
        }
        dlg.add(form, BorderLayout.CENTER);

        JPanel btns = darkPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(PANEL_BG);
        JButton save = accentButton("Save"); JButton cancel = ghostButton("Cancel");
        btns.add(cancel); btns.add(save);
        dlg.add(btns, BorderLayout.SOUTH);

        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String name = nameF.getText().trim();
            String msStr = msF.getText().trim();
            String prStr = priceF.getText().trim();
            if (name.isEmpty() || msStr.isEmpty() || prStr.isEmpty()
                    || albumCB.getSelectedItem()==null || genreCB.getSelectedItem()==null || mediaCB.getSelectedItem()==null) {
                JOptionPane.showMessageDialog(dlg,"Please fill in all fields.","Validation",JOptionPane.WARNING_MESSAGE); return;
            }
            try {
                int albumId = Integer.parseInt(((String[])albumCB.getSelectedItem())[0]);
                int genreId = Integer.parseInt(((String[])genreCB.getSelectedItem())[0]);
                int mediaId = Integer.parseInt(((String[])mediaCB.getSelectedItem())[0]);
                int ms = Integer.parseInt(msStr);
                double price = Double.parseDouble(prStr);
                String ins = "INSERT INTO Track (Name, AlbumId, MediaTypeId, GenreId, Milliseconds, Bytes, UnitPrice) VALUES (?,?,?,?,?,0,?)";
                try (PreparedStatement ps = conn.prepareStatement(ins)) {
                    ps.setString(1,name); ps.setInt(2,albumId); ps.setInt(3,mediaId);
                    ps.setInt(4,genreId); ps.setInt(5,ms); ps.setDouble(6,price);
                    ps.executeUpdate();
                }
                setStatus("Track '" + name + "' added successfully.");
                loadTracks(trackSearch.getText().trim());
                dlg.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg,"Duration must be an integer; Price must be a number.","Validation",JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dlg,"DB error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
            }
        });
        dlg.setVisible(true);
    }

    // ── TAB 3: REPORT ─────────────────────────────────────────────────────────
    private DefaultTableModel reportModel;

    private JPanel buildReportTab() {
        JPanel p = darkPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(16,16,16,16));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel lbl = styledLabel("Genre Revenue Report - auto-generated on tab open");
        lbl.setForeground(SUBTEXT);
        JButton btn = accentButton("Refresh");
        top.add(lbl); top.add(btn);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"Rank","Genre","Total Revenue ($)"};
        reportModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} };
        p.add(styledScroll(styledTable(reportModel)), BorderLayout.CENTER);
        btn.addActionListener(e -> refreshReport());
        return p;
    }

    private void refreshReport() {
        // Advanced SQL: RANK() window function
        String sql = "SELECT RANK() OVER (ORDER BY SUM(il.UnitPrice * il.Quantity) DESC) AS Rank, " +
                     "g.Name AS Genre, ROUND(SUM(il.UnitPrice * il.Quantity), 2) AS Revenue " +
                     "FROM InvoiceLine il " +
                     "JOIN Track t ON il.TrackId = t.TrackId " +
                     "JOIN Genre g ON t.GenreId = g.GenreId " +
                     "GROUP BY g.GenreId, g.Name " +
                     "ORDER BY Revenue DESC";
        populateTable(reportModel, sql);
        setStatus("Report refreshed - " + new java.util.Date());
    }

    // ── TAB 4: NOTIFICATIONS ──────────────────────────────────────────────────
    private DefaultTableModel custModel;
    private JTable custTable;
    private JTextField custSearch;
    private DefaultTableModel inactiveModel;

    private JPanel buildNotificationsTab() {
        JPanel p = darkPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(16,16,16,16));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildCRUDPanel(), buildInactivePanel());
        split.setResizeWeight(0.6);
        split.setDividerSize(6);
        split.setBackground(BG);
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCRUDPanel() {
        JPanel p = darkPanel(new BorderLayout(8, 8));
        p.setBorder(titledBorder("Customer Management (CRUD)"));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        custSearch = styledField(22);
        JButton search = accentButton("Search");
        JButton add  = accentButton("+ Add");
        JButton edit = accentButton("Edit");
        JButton del  = dangerButton("Delete");
        top.add(styledLabel("Search:")); top.add(custSearch); top.add(search); top.add(add); top.add(edit); top.add(del);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"ID","First Name","Last Name","Email","Phone","Country"};
        custModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r,int c){return false;} };
        custTable = styledTable(custModel);
        custTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(custTable), BorderLayout.CENTER);
        search.addActionListener(e -> loadCustomers(custSearch.getText().trim()));
        custSearch.addActionListener(e -> loadCustomers(custSearch.getText().trim()));
        add.addActionListener(e -> showCustomerDialog(null));
        edit.addActionListener(e -> {
            int row = custTable.getSelectedRow();
            if (row < 0) { alert("Select a customer to edit."); return; }
            showCustomerDialog(Integer.parseInt(custModel.getValueAt(row,0).toString()));
        });
        del.addActionListener(e -> {
            int row = custTable.getSelectedRow();
            if (row < 0) { alert("Select a customer to delete."); return; }
            int id = Integer.parseInt(custModel.getValueAt(row,0).toString());
            String name = custModel.getValueAt(row,1)+" "+custModel.getValueAt(row,2);
            if (JOptionPane.showConfirmDialog(this,"Delete customer '"+name+"'?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                try { exec("DELETE FROM Customer WHERE CustomerId=?", id); loadCustomers(custSearch.getText().trim()); setStatus("Deleted: "+name); }
                catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Cannot delete - customer has linked invoices.\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        });
        return p;
    }

    private void loadCustomers(String filter) {
        String sql = "SELECT CustomerId, FirstName, LastName, Email, Phone, Country FROM Customer " +
                     "WHERE LOWER(CONCAT(FirstName,' ',LastName)) LIKE ? OR LOWER(Email) LIKE ? OR LOWER(Country) LIKE ? " +
                     "ORDER BY LastName, FirstName";
        String like = "%" + filter.toLowerCase() + "%";
        populateTable(custModel, sql, like, like, like);
    }

    private void showCustomerDialog(Integer customerId) {
        boolean isEdit = (customerId != null);
        JDialog dlg = new JDialog(this, isEdit ? "Edit Customer" : "New Customer", true);
        dlg.setSize(430, 360);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(PANEL_BG);
        dlg.setLayout(new BorderLayout(10,10));
        JPanel form = darkPanel(new GridBagLayout());
        form.setBackground(PANEL_BG);
        form.setBorder(new EmptyBorder(20,24,10,24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField firstF=styledField(20), lastF=styledField(20), emailF=styledField(20), phoneF=styledField(20), countryF=styledField(20);
        String[] labels = {"First Name:","Last Name:","Email:","Phone:","Country:"};
        Component[] fields = {firstF,lastF,emailF,phoneF,countryF};
        for (int i=0;i<labels.length;i++) { gbc.gridx=0;gbc.gridy=i;gbc.weightx=0;form.add(styledLabel(labels[i]),gbc); gbc.gridx=1;gbc.weightx=1;form.add(fields[i],gbc); }
        dlg.add(form, BorderLayout.CENTER);
        if (isEdit) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT FirstName,LastName,Email,Phone,Country FROM Customer WHERE CustomerId=?")) {
                ps.setInt(1,customerId); ResultSet rs=ps.executeQuery();
                if (rs.next()) { firstF.setText(rs.getString(1)); lastF.setText(rs.getString(2)); emailF.setText(rs.getString(3)); phoneF.setText(rs.getString(4)==null?"":rs.getString(4)); countryF.setText(rs.getString(5)==null?"":rs.getString(5)); }
            } catch (SQLException ex) { ex.printStackTrace(); }
        }
        JPanel btns = darkPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        btns.setBackground(PANEL_BG);
        JButton save=accentButton(isEdit?"Update":"Create"); JButton cancel=ghostButton("Cancel");
        btns.add(cancel); btns.add(save);
        dlg.add(btns, BorderLayout.SOUTH);
        cancel.addActionListener(e -> dlg.dispose());
        save.addActionListener(e -> {
            String first=firstF.getText().trim(), last=lastF.getText().trim(), email=emailF.getText().trim(), phone=phoneF.getText().trim(), country=countryF.getText().trim();
            if (first.isEmpty()||last.isEmpty()||email.isEmpty()) { alert("First name, last name, and email are required."); return; }
            try {
                if (isEdit) { exec("UPDATE Customer SET FirstName=?,LastName=?,Email=?,Phone=?,Country=? WHERE CustomerId=?",first,last,email,phone,country,customerId); setStatus("Updated: "+first+" "+last); }
                else {
                    int nextId;
                    try (Statement st=conn.createStatement(); ResultSet rs=st.executeQuery("SELECT MAX(CustomerId)+1 FROM Customer")) { rs.next(); nextId=rs.getInt(1); }
                    exec("INSERT INTO Customer (CustomerId,FirstName,LastName,Email,Phone,Country) VALUES (?,?,?,?,?,?)",nextId,first,last,email,phone,country);
                    setStatus("Created: "+first+" "+last);
                }
                loadCustomers(custSearch.getText().trim()); dlg.dispose();
            } catch (SQLException ex) { JOptionPane.showMessageDialog(dlg,"DB error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        });
        dlg.setVisible(true);
    }

    private JPanel buildInactivePanel() {
        JPanel p = darkPanel(new BorderLayout(8,8));
        p.setBorder(titledBorder("Inactive Customers (no invoices or last invoice > 2 years ago)"));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT,8,4));
        JTextField search = styledField(22);
        JButton btn=accentButton("Search"); JButton ref=ghostButton("Refresh");
        top.add(styledLabel("Search:")); top.add(search); top.add(btn); top.add(ref);
        p.add(top, BorderLayout.NORTH);
        String[] cols = {"ID","First Name","Last Name","Email","Country","Last Invoice"};
        inactiveModel = new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} };
        p.add(styledScroll(styledTable(inactiveModel)), BorderLayout.CENTER);
        btn.addActionListener(e -> loadInactive(search.getText().trim()));
        search.addActionListener(e -> loadInactive(search.getText().trim()));
        ref.addActionListener(e -> loadInactive(search.getText().trim()));
        loadInactive("");
        return p;
    }

    private void loadInactive(String filter) {
        // Advanced SQL: DATE_SUB with INTERVAL 2 YEAR
        String sql = "SELECT c.CustomerId, c.FirstName, c.LastName, c.Email, c.Country, " +
                     "COALESCE(CAST(MAX(i.InvoiceDate) AS CHAR),'Never') AS LastInvoice " +
                     "FROM Customer c LEFT JOIN Invoice i ON c.CustomerId = i.CustomerId " +
                     "WHERE (LOWER(CONCAT(c.FirstName,' ',c.LastName)) LIKE ? OR LOWER(c.Email) LIKE ?) " +
                     "GROUP BY c.CustomerId, c.FirstName, c.LastName, c.Email, c.Country " +
                     "HAVING MAX(i.InvoiceDate) IS NULL OR MAX(i.InvoiceDate) < DATE_SUB(NOW(), INTERVAL 2 YEAR) " +
                     "ORDER BY LastInvoice ASC";
        String like = "%" + filter.toLowerCase() + "%";
        populateTable(inactiveModel, sql, like, like);
    }

    // ── TAB 5: RECOMMENDATIONS ────────────────────────────────────────────────
    private JComboBox custCombo;
    private JLabel spentLbl, purchasesLbl, lastPurchLbl, genreLbl;
    private DefaultTableModel recModel;

    private JPanel buildRecommendationsTab() {
        JPanel p = darkPanel(new BorderLayout(12,12));
        p.setBorder(new EmptyBorder(16,16,16,16));
        JPanel top = darkPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        custCombo = new JComboBox(); custCombo.setBackground(BG); custCombo.setForeground(TEXT); custCombo.setFont(FONT_BODY);
        custCombo.setPreferredSize(new Dimension(300,32));
        custCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList l,Object v,int i,boolean s,boolean f) {
                String[] arr=(String[])v; JLabel lb=new JLabel(arr==null?"":arr[1]);
                lb.setForeground(TEXT); lb.setBackground(s?ACCENT:BG); lb.setOpaque(true); return lb;
            }
        });
        loadCombo(custCombo,"SELECT CustomerId, CONCAT(FirstName,' ',LastName) FROM Customer ORDER BY LastName, FirstName");
        top.add(styledLabel("Customer:")); top.add(custCombo);
        JButton load = accentButton("Load Insights");
        top.add(load);
        p.add(top, BorderLayout.NORTH);

        JPanel cards = darkPanel(new GridLayout(1,4,12,0));
        spentLbl     = statCard(cards,"Total Spent","--");
        purchasesLbl = statCard(cards,"Purchases","--");
        lastPurchLbl = statCard(cards,"Last Purchase","--");
        genreLbl     = statCard(cards,"Favourite Genre","--");
        p.add(cards, BorderLayout.CENTER);

        JPanel bot = darkPanel(new BorderLayout(8,8));
        bot.setBorder(titledBorder("Recommended Tracks (based on favourite genre, not yet purchased)"));
        String[] cols = {"TrackId","Track Name","Artist","Album","Genre","Price ($)"};
        recModel = new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} };
        bot.add(styledScroll(styledTable(recModel)), BorderLayout.CENTER);
        p.add(bot, BorderLayout.SOUTH);
        bot.setPreferredSize(new Dimension(0,320));

        load.addActionListener(e -> loadInsights());
        return p;
    }

    private void loadInsights() {
        String[] item = (String[]) custCombo.getSelectedItem();
        if (item==null) return;
        int cid = Integer.parseInt(item[0]);

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ROUND(SUM(i.Total),2), COUNT(DISTINCT i.InvoiceId), MAX(i.InvoiceDate) FROM Invoice i WHERE i.CustomerId=?")) {
            ps.setInt(1,cid); ResultSet rs=ps.executeQuery();
            if (rs.next()) { spentLbl.setText("$"+(rs.getString(1)==null?"0.00":rs.getString(1))); purchasesLbl.setText(rs.getString(2)==null?"0":rs.getString(2)); lastPurchLbl.setText(rs.getString(3)==null?"--":rs.getString(3).substring(0,10)); }
        } catch (SQLException e) { e.printStackTrace(); }

        String favGenre="--"; int favGenreId=-1;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT g.GenreId, g.Name FROM InvoiceLine il JOIN Invoice i ON il.InvoiceId=i.InvoiceId JOIN Track t ON il.TrackId=t.TrackId JOIN Genre g ON t.GenreId=g.GenreId WHERE i.CustomerId=? GROUP BY g.GenreId,g.Name ORDER BY COUNT(*) DESC LIMIT 1")) {
            ps.setInt(1,cid); ResultSet rs=ps.executeQuery();
            if (rs.next()) { favGenreId=rs.getInt(1); favGenre=rs.getString(2); }
        } catch (SQLException e) { e.printStackTrace(); }
        genreLbl.setText(favGenre);

        recModel.setRowCount(0);
        if (favGenreId==-1) return;
        // Advanced SQL: NOT IN subquery + ORDER BY RAND()
        String recSql = "SELECT t.TrackId, t.Name, ar.Name AS Artist, al.Title AS Album, g.Name AS Genre, t.UnitPrice " +
                        "FROM Track t JOIN Album al ON t.AlbumId=al.AlbumId JOIN Artist ar ON al.ArtistId=ar.ArtistId JOIN Genre g ON t.GenreId=g.GenreId " +
                        "WHERE t.GenreId=? AND t.TrackId NOT IN (SELECT il.TrackId FROM InvoiceLine il JOIN Invoice i ON il.InvoiceId=i.InvoiceId WHERE i.CustomerId=?) " +
                        "ORDER BY RAND() LIMIT 20";
        try (PreparedStatement ps=conn.prepareStatement(recSql)) {
            ps.setInt(1,favGenreId); ps.setInt(2,cid); ResultSet rs=ps.executeQuery();
            while(rs.next()) recModel.addRow(new Object[]{rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getDouble(6)});
        } catch (SQLException e) { e.printStackTrace(); }
        setStatus("Loaded insights for "+item[1]+" - "+recModel.getRowCount()+" recommendations");
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private void populateTable(DefaultTableModel model, String sql, Object... params) {
        model.setRowCount(0);
        try (PreparedStatement ps=conn.prepareStatement(sql)) {
            for (int i=0;i<params.length;i++) ps.setObject(i+1,params[i]);
            ResultSet rs=ps.executeQuery(); ResultSetMetaData md=rs.getMetaData(); int cols=md.getColumnCount();
            while(rs.next()) { Object[] row=new Object[cols]; for(int c=1;c<=cols;c++) row[c-1]=rs.getObject(c); model.addRow(row); }
        } catch (SQLException e) { setStatus("Query error: "+e.getMessage()); e.printStackTrace(); }
    }

    private void exec(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps=conn.prepareStatement(sql)) { for(int i=0;i<params.length;i++) ps.setObject(i+1,params[i]); ps.executeUpdate(); }
    }

    private void loadCombo(JComboBox combo, String sql) {
        try (Statement st=conn.createStatement(); ResultSet rs=st.executeQuery(sql)) {
            while(rs.next()) combo.addItem(new String[]{rs.getString(1),rs.getString(2)});
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void setStatus(String msg) { statusBar.setText("  "+msg); }
    private void alert(String msg) { JOptionPane.showMessageDialog(this,msg,"Notice",JOptionPane.INFORMATION_MESSAGE); }

    // ── STYLE HELPERS ─────────────────────────────────────────────────────────
    private JPanel darkPanel(LayoutManager lm) { JPanel p=new JPanel(lm); p.setBackground(BG); return p; }
    private JLabel styledLabel(String text) { JLabel l=new JLabel(text); l.setForeground(TEXT); l.setFont(FONT_LABEL); return l; }
    private JTextField styledField(int cols) {
        JTextField f=new JTextField(cols); f.setBackground(PANEL_BG); f.setForeground(TEXT); f.setCaretColor(TEXT); f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_C,1),BorderFactory.createEmptyBorder(4,8,4,8))); return f;
    }
    private JButton accentButton(String text) {
        JButton b=new JButton(text); b.setBackground(ACCENT); b.setForeground(Color.WHITE); b.setFont(FONT_LABEL); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT2,1),BorderFactory.createEmptyBorder(6,14,6,14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(ACCENT2);} public void mouseExited(MouseEvent e){b.setBackground(ACCENT);} });
        return b;
    }
    private JButton ghostButton(String text) {
        JButton b=new JButton(text); b.setBackground(PANEL_BG); b.setForeground(SUBTEXT); b.setFont(FONT_BODY); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_C,1),BorderFactory.createEmptyBorder(6,12,6,12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private JButton dangerButton(String text) {
        JButton b=accentButton(text); b.setBackground(DANGER);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DANGER.darker(),1),BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new MouseAdapter() { public void mouseEntered(MouseEvent e){b.setBackground(DANGER.darker());} public void mouseExited(MouseEvent e){b.setBackground(DANGER);} });
        return b;
    }
    private JTable styledTable(DefaultTableModel model) {
        JTable t=new JTable(model); t.setBackground(BG); t.setForeground(TEXT); t.setFont(FONT_BODY); t.setGridColor(BORDER_C); t.setRowHeight(28);
        t.setSelectionBackground(ACCENT); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(PANEL_BG); t.getTableHeader().setForeground(SUBTEXT); t.getTableHeader().setFont(FONT_LABEL);
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tbl,Object val,boolean sel,boolean foc,int row,int col) {
                super.getTableCellRendererComponent(tbl,val,sel,foc,row,col);
                if(!sel) setBackground(row%2==0?BG:ROW_ALT); setForeground(sel?Color.WHITE:TEXT);
                setBorder(BorderFactory.createEmptyBorder(0,8,0,8)); return this;
            }
        }); return t;
    }
    private JScrollPane styledScroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBackground(new Color(18,18,30));
        sp.getViewport().setBackground(new Color(18,18,30));
        sp.setBorder(BorderFactory.createLineBorder(new Color(55,55,80),1));
        sp.getVerticalScrollBar().setBackground(new Color(28,28,46));
        sp.getHorizontalScrollBar().setBackground(new Color(28,28,46));
        return sp;
    }
    private Border titledBorder(String title) {
        TitledBorder b=BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER_C,1),title);
        b.setTitleColor(SUBTEXT); b.setTitleFont(FONT_SMALL);
        return BorderFactory.createCompoundBorder(b,new EmptyBorder(6,6,6,6));
    }
    private JLabel statCard(JPanel parent, String labelText, String initialValue) {
        JPanel card=new JPanel(new BorderLayout(4,4)); card.setBackground(PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER_C,1),BorderFactory.createEmptyBorder(16,16,16,16)));
        JLabel title=new JLabel(labelText,SwingConstants.CENTER); title.setFont(FONT_SMALL); title.setForeground(SUBTEXT);
        JLabel val=new JLabel(initialValue,SwingConstants.CENTER); val.setFont(new Font("Segoe UI",Font.BOLD,20)); val.setForeground(ACCENT2);
        card.add(title,BorderLayout.NORTH); card.add(val,BorderLayout.CENTER); parent.add(card); return val;
    }

    public static void main(String[] args) {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) {
            try { Class.forName("org.mariadb.jdbc.Driver"); }
            catch (ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(null,"JDBC driver not found.","Driver Error",JOptionPane.ERROR_MESSAGE); System.exit(1);
            }
        }
        applyDarkTheme();
        SwingUtilities.invokeLater(new Runnable() { public void run() { new ChinookApp(); } });
    }

    private static void applyDarkTheme() {
        Color bg       = new Color(18, 18, 30);
        Color panelBg  = new Color(28, 28, 46);
        Color text     = new Color(226, 232, 240);
        Color subtext  = new Color(148, 163, 184);
        Color border   = new Color(55, 55, 80);
        Color accent   = new Color(99, 102, 241);
        Color rowAlt   = new Color(38, 38, 60);
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Panel.background",           bg);
        UIManager.put("OptionPane.background",      panelBg);
        UIManager.put("OptionPane.messageForeground",text);
        UIManager.put("Table.background",           bg);
        UIManager.put("Table.foreground",           text);
        UIManager.put("Table.gridColor",            border);
        UIManager.put("Table.selectionBackground",  accent);
        UIManager.put("Table.selectionForeground",  Color.WHITE);
        UIManager.put("TableHeader.background",     panelBg);
        UIManager.put("TableHeader.foreground",     subtext);
        UIManager.put("ScrollPane.background",      bg);
        UIManager.put("Viewport.background",        bg);
        UIManager.put("ScrollBar.background",       panelBg);
        UIManager.put("ScrollBar.thumb",            border);
        UIManager.put("ScrollBar.track",            panelBg);
        UIManager.put("TabbedPane.background",      bg);
        UIManager.put("TabbedPane.foreground",      text);
        UIManager.put("TabbedPane.selected",        panelBg);
        UIManager.put("TabbedPane.contentAreaColor",panelBg);
        UIManager.put("TabbedPane.tabAreaBackground",bg);
        UIManager.put("TabbedPane.unselectedBackground", bg);
        UIManager.put("TabbedPane.shadow",          border);
        UIManager.put("TabbedPane.darkShadow",      border);
        UIManager.put("TabbedPane.light",           panelBg);
        UIManager.put("TabbedPane.highlight",       panelBg);
        UIManager.put("TabbedPane.focus",           accent);
        UIManager.put("TextField.background",       panelBg);
        UIManager.put("TextField.foreground",       text);
        UIManager.put("TextField.caretForeground",  text);
        UIManager.put("TextField.border",           BorderFactory.createLineBorder(border,1));
        UIManager.put("ComboBox.background",        panelBg);
        UIManager.put("ComboBox.foreground",        text);
        UIManager.put("ComboBox.selectionBackground",accent);
        UIManager.put("ComboBox.selectionForeground",Color.WHITE);
        UIManager.put("ComboBox.buttonBackground",  panelBg);
        UIManager.put("List.background",            panelBg);
        UIManager.put("List.foreground",            text);
        UIManager.put("List.selectionBackground",   accent);
        UIManager.put("List.selectionForeground",   Color.WHITE);
        UIManager.put("Button.background",          panelBg);
        UIManager.put("Button.foreground",          text);
        UIManager.put("Label.foreground",           text);
        UIManager.put("SplitPane.background",       bg);
        UIManager.put("SplitPane.dividerSize",      6);
        UIManager.put("SplitPaneDivider.background",border);
        UIManager.put("Dialog.background",          panelBg);
        UIManager.put("PopupMenu.background",       panelBg);
        UIManager.put("PopupMenu.foreground",       text);
        UIManager.put("MenuItem.background",        panelBg);
        UIManager.put("MenuItem.foreground",        text);
        UIManager.put("MenuItem.selectionBackground",accent);
    }
}
