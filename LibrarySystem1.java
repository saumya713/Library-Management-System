package mypack;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.time.LocalDate;

public class LibrarySystem1 {
    private static final String URL = "jdbc:mysql://localhost:3306/library_system";
    private static final String USER = "root";
    private static final String PASS = "abcd1234";
 
    public static void main(String[] args) {
        new LibrarySystem1();
    }

    public LibrarySystem1() {
        JFrame f = new JFrame("Library Management System");
        f.setSize(950, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // ==================== SHARED MODELS ====================
        String[] ledgerCols = {"Txn ID", "Book Title", "Member Name", "Issue Date", "Due Date", "Return Date", "Fine (₹)", "Status"};
        DefaultTableModel ledgerModel = new DefaultTableModel(ledgerCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // ==================== TAB 1: BOOK CATALOG ====================
        JPanel catalogPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setBackground(new Color(240, 244, 248));

        JLabel branchLabel = new JLabel("Select Branch:");
        JComboBox<String> branches = new JComboBox<String>();
        branches.addItem("All Branches");
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT branch_name FROM branches");
            while (rs.next()) {
                branches.addItem(rs.getString("branch_name"));
            }
            rs.close(); s.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }

        JLabel searchLabel = new JLabel("Search Title/Author:");
        JTextField searchField = new JTextField(15);
        JButton searchButton = new JButton("Search");

        topPanel.add(branchLabel);
        topPanel.add(branches);
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        topPanel.add(searchButton);

        String[] columns = {"Book ID", "Title", "Author", "Category", "Branch", "Available Copies"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable bookTable = new JTable(tableModel);
        bookTable.setRowHeight(24);

        loadBooks(tableModel, branches, searchField);

        ActionListener filterListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadBooks(tableModel, branches, searchField);
            }
        };
        branches.addActionListener(filterListener);
        searchField.addActionListener(filterListener);
        searchButton.addActionListener(filterListener);

        JPanel bottomActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton updateStockBtn = new JButton("Update Stock Copies");
        JButton deleteBookBtn = new JButton("Delete Selected Book");
        bottomActionPanel.add(updateStockBtn);
        bottomActionPanel.add(deleteBookBtn);

        updateStockBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = bookTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(f, "Please select a book from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int bookId = (int) bookTable.getValueAt(selectedRow, 0);
                String currentCopies = bookTable.getValueAt(selectedRow, 5).toString();
                String input = JOptionPane.showInputDialog(f, "Enter new available copies count:", currentCopies);
                if (input == null) return;
                try {
                    int newCopies = Integer.parseInt(input.trim());
                    Connection con = DriverManager.getConnection(URL, USER, PASS);
                    PreparedStatement ps = con.prepareStatement("UPDATE books SET available_copies = ? WHERE book_id = ?");
                    ps.setInt(1, newCopies);
                    ps.setInt(2, bookId);
                    ps.executeUpdate();
                    ps.close(); con.close();
                    JOptionPane.showMessageDialog(f, "Stock updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadBooks(tableModel, branches, searchField);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(f, "Invalid number format!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteBookBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = bookTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(f, "Please select a book from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int bookId = (int) bookTable.getValueAt(selectedRow, 0);
                String title = (String) bookTable.getValueAt(selectedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(f, "Are you sure you want to delete book: " + title + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        Connection con = DriverManager.getConnection(URL, USER, PASS);
                        PreparedStatement pstmt = con.prepareStatement("DELETE FROM books WHERE book_id = ?");
                        pstmt.setInt(1, bookId);
                        pstmt.executeUpdate();
                        pstmt.close(); con.close();
                        JOptionPane.showMessageDialog(f, "Book deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadBooks(tableModel, branches, searchField);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(f, "Cannot delete book. It may have active transaction history.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        catalogPanel.add(topPanel, BorderLayout.NORTH);
        catalogPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        catalogPanel.add(bottomActionPanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Book Catalogue", catalogPanel);

        // ==================== TAB 2: MEMBER MANAGEMENT ====================
        JPanel memberPanel = new JPanel(new BorderLayout(15, 15));
        memberPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Manage Members"));

        JTextField nameField = new JTextField(15);
        JTextField emailField = new JTextField(15);
        JTextField phoneField = new JTextField(15);
        JComboBox<String> typeCombo = new JComboBox<String>(new String[]{"Student", "Faculty"});

        formPanel.add(new JLabel("Full Name:"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("Phone:"));
        formPanel.add(phoneField);
        formPanel.add(new JLabel("Member Type:"));
        formPanel.add(typeCombo);

        JButton registerBtn = new JButton("Register Member");
        JButton deleteMemberBtn = new JButton("Remove Member");
        formPanel.add(registerBtn);
        formPanel.add(deleteMemberBtn);

        String[] memberCols = {"Member ID", "Name", "Email", "Phone", "Type"};
        DefaultTableModel memberModel = new DefaultTableModel(memberCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable memberTable = new JTable(memberModel);
        memberTable.setRowHeight(24);

        loadMembers(memberModel);

        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String phone = phoneField.getText().trim();
                String type = (String) typeCombo.getSelectedItem();
                if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                    JOptionPane.showMessageDialog(f, "Please fill all fields.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    Connection con = DriverManager.getConnection(URL, USER, PASS);
                    PreparedStatement pstmt = con.prepareStatement("INSERT INTO members (name, email, phone, membership_type) VALUES (?, ?, ?, ?)");
                    pstmt.setString(1, name);
                    pstmt.setString(2, email);
                    pstmt.setString(3, phone);
                    pstmt.setString(4, type);
                    pstmt.executeUpdate();
                    pstmt.close(); con.close();
                    JOptionPane.showMessageDialog(f, "Member registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    nameField.setText(""); emailField.setText(""); phoneField.setText("");
                    loadMembers(memberModel);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(f, "Error registering member: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteMemberBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = memberTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(f, "Please select a member from the table to remove.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int memberId = (int) memberTable.getValueAt(selectedRow, 0);
                String memberName = (String) memberTable.getValueAt(selectedRow, 1);
                int confirm = JOptionPane.showConfirmDialog(f, "Are you sure you want to remove member: " + memberName + "?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        Connection con = DriverManager.getConnection(URL, USER, PASS);
                        PreparedStatement ps = con.prepareStatement("DELETE FROM members WHERE member_id = ?");
                        ps.setInt(1, memberId);
                        ps.executeUpdate();
                        ps.close(); con.close();
                        JOptionPane.showMessageDialog(f, "Member removed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadMembers(memberModel);
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(f, "Cannot remove member. They may have active book loans.", "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        memberPanel.add(formPanel, BorderLayout.WEST);
        memberPanel.add(new JScrollPane(memberTable), BorderLayout.CENTER);
        tabbedPane.addTab("Members", memberPanel);

        // ==================== TAB 3: CIRCULATION DESK ====================
        JPanel circPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        circPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel issuePanel = new JPanel();
        issuePanel.setLayout(new BoxLayout(issuePanel, BoxLayout.Y_AXIS));
        issuePanel.setBorder(BorderFactory.createTitledBorder("Issue Books"));

        JComboBox<String> issueBookCombo = new JComboBox<String>();
        JComboBox<String> issueMemberCombo = new JComboBox<String>();
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            Statement s = con.createStatement();
            ResultSet rsBooks = s.executeQuery("SELECT book_id, title FROM books WHERE available_copies > 0");
            while (rsBooks.next()) {
                issueBookCombo.addItem(rsBooks.getInt("book_id") + " - " + rsBooks.getString("title"));
            }
            rsBooks.close();
            ResultSet rsMembers = s.executeQuery("SELECT member_id, name FROM members");
            while (rsMembers.next()) {
                issueMemberCombo.addItem(rsMembers.getInt("member_id") + " - " + rsMembers.getString("name"));
            }
            rsMembers.close(); s.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }

        issueBookCombo.setPreferredSize(new Dimension(250, 28));
        issueMemberCombo.setPreferredSize(new Dimension(250, 28));

        JPanel issueRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        issueRow1.add(new JLabel("Book:"));
        issueRow1.add(issueBookCombo);

        JPanel issueRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        issueRow2.add(new JLabel("Member:"));
        issueRow2.add(issueMemberCombo);

        JPanel issueRow3 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton issueBtn = new JButton("Issue Book");
        issueRow3.add(issueBtn);

        issuePanel.add(issueRow1);
        issuePanel.add(issueRow2);
        issuePanel.add(issueRow3);

        JPanel returnPanel = new JPanel();
        returnPanel.setLayout(new BoxLayout(returnPanel, BoxLayout.Y_AXIS));
        returnPanel.setBorder(BorderFactory.createTitledBorder("Return Books"));

        JComboBox<String> returnTxnCombo = new JComboBox<String>();
        returnTxnCombo.setPreferredSize(new Dimension(250, 28));
        
        // Load active loans dropdown initially
        loadReturnDropdown(returnTxnCombo);

        issueBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedBook = (String) issueBookCombo.getSelectedItem();
                String selectedMember = (String) issueMemberCombo.getSelectedItem();
                if (selectedBook == null || selectedMember == null) {
                    JOptionPane.showMessageDialog(f, "Please select both a book and a member.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int bookId = Integer.parseInt(selectedBook.split(" - ")[0]);
                int memberId = Integer.parseInt(selectedMember.split(" - ")[0]);
                try {
                    Connection con = DriverManager.getConnection(URL, USER, PASS);
                    con.setAutoCommit(false);
                    PreparedStatement psTxn = con.prepareStatement("INSERT INTO transactions (book_id, member_id, issue_date, due_date, status) VALUES (?, ?, ?, ?, ?)");
                    psTxn.setInt(1, bookId);
                    psTxn.setInt(2, memberId);
                    psTxn.setString(3, LocalDate.now().toString());
                    psTxn.setString(4, LocalDate.now().plusDays(14).toString());
                    psTxn.setString(5, "ISSUED");
                    psTxn.executeUpdate(); psTxn.close();

                    PreparedStatement pstUpd = con.prepareStatement("UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?");
                    pstUpd.setInt(1, bookId);
                    pstUpd.executeUpdate(); pstUpd.close();

                    con.commit(); con.setAutoCommit(true); con.close();
                    
                    // Automatically update both ledger and return dropdown instantly!
                    loadLedger(ledgerModel);
                    loadReturnDropdown(returnTxnCombo);

                    JOptionPane.showMessageDialog(f, "Book issued successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(f, "Error issuing book: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JPanel returnRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        returnRow1.add(new JLabel("Active Loan:"));
        returnRow1.add(returnTxnCombo);

        JPanel returnRow2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton returnBtn = new JButton("Return Book");
        returnRow2.add(returnBtn);

        returnPanel.add(returnRow1);
        returnPanel.add(returnRow2);

        returnBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedTxn = (String) returnTxnCombo.getSelectedItem();
                if (selectedTxn == null) {
                    JOptionPane.showMessageDialog(f, "No active issued book selected for return.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int txnId = Integer.parseInt(selectedTxn.split(" \\| ")[0]);
                String inputDate = JOptionPane.showInputDialog(f, "Enter return date (YYYY-MM-DD):", LocalDate.now().toString());
                if (inputDate == null) return;
                LocalDate returnDate;
                try {
                    returnDate = LocalDate.parse(inputDate.trim());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(f, "Invalid date format! Please use YYYY-MM-DD.", "Date Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    Connection con = DriverManager.getConnection(URL, USER, PASS);
                    con.setAutoCommit(false);
                    PreparedStatement psSel = con.prepareStatement("SELECT book_id, due_date FROM transactions WHERE transaction_id = ?");
                    psSel.setInt(1, txnId);
                    ResultSet rs = psSel.executeQuery();
                    int bookId = 0;
                    LocalDate dueDate = LocalDate.now();
                    if (rs.next()) {
                        bookId = rs.getInt("book_id");
                        dueDate = LocalDate.parse(rs.getString("due_date"));
                    }
                    rs.close(); psSel.close();

                    long daysLate = 0;
                    LocalDate tempDate = dueDate;
                    while (tempDate.isBefore(returnDate)) {
                        daysLate++;
                        tempDate = tempDate.plusDays(1);
                    }
                    double fineAmount = daysLate > 0 ? daysLate * 5.0 : 0.0;

                    PreparedStatement psUpTxn = con.prepareStatement("UPDATE transactions SET return_date = ?, fine_amount = ?, status = 'RETURNED' WHERE transaction_id = ?");
                    psUpTxn.setString(1, returnDate.toString());
                    psUpTxn.setDouble(2, fineAmount);
                    psUpTxn.setInt(3, txnId);
                    psUpTxn.executeUpdate(); psUpTxn.close();

                    PreparedStatement pstUpBook = con.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?");
                    pstUpBook.setInt(1, bookId);
                    pstUpBook.executeUpdate(); pstUpBook.close();

                    con.commit(); con.setAutoCommit(true); con.close();

                 
                    loadLedger(ledgerModel);
                    loadReturnDropdown(returnTxnCombo);

                    String message = "Book returned successfully on " + returnDate + "!";
                    if (fineAmount > 0) {
                        message += "\nOverdue by " + daysLate + " days.\nAssessed Fine: ₹" + fineAmount;
                    } else {
                        message += "\nReturned on time. No fine incurred.";
                    }
                    JOptionPane.showMessageDialog(f, message, "Return Processed", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(f, "Error processing return: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        circPanel.add(issuePanel);
        circPanel.add(returnPanel);
        tabbedPane.addTab("Circulation Desk", circPanel);

        // ==================== TAB 4: TRANSACTION LEDGER ====================
        JPanel ledgerPanel = new JPanel(new BorderLayout(10, 10));
        ledgerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTable ledgerTable = new JTable(ledgerModel);
        ledgerTable.setRowHeight(25);

        loadLedger(ledgerModel);

        ledgerPanel.add(new JScrollPane(ledgerTable), BorderLayout.CENTER);
        tabbedPane.addTab("Transaction Ledger", ledgerPanel);

        f.add(tabbedPane);
        f.setVisible(true);
    }

    // ==================== HELPER METHODS ====================

    private void loadBooks(DefaultTableModel tableModel, JComboBox<String> branches, JTextField searchField) {
        tableModel.setRowCount(0);
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            String branchFilter = (String) branches.getSelectedItem();
            if (branchFilter == null) branchFilter = "All Branches";
            String keyword = searchField.getText().trim();
            StringBuilder query = new StringBuilder(
                "SELECT b.book_id, b.title, b.author, b.category, br.branch_name, b.available_copies " +
                "FROM books b JOIN branches br ON b.branch_id = br.branch_id WHERE 1=1"
            );
            if (!branchFilter.equals("All Branches")) {
                query.append(" AND br.branch_name = ?");
            }
            if (!keyword.isEmpty()) {
                query.append(" AND (b.title LIKE ? OR b.author LIKE ?)");
            }

            PreparedStatement ps = con.prepareStatement(query.toString());
            int paramIndex = 1;
            if (!branchFilter.equals("All Branches")) {
                ps.setString(paramIndex++, branchFilter);
            }
            if (!keyword.isEmpty()) {
                ps.setString(paramIndex++, "%" + keyword + "%");
                ps.setString(paramIndex++, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("book_id"), rs.getString("title"), rs.getString("author"),
                    rs.getString("category"), rs.getString("branch_name"), rs.getInt("available_copies")
                });
            }
            rs.close(); ps.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadMembers(DefaultTableModel memberModel) {
        memberModel.setRowCount(0);
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM members");
            while (rs.next()) {
                memberModel.addRow(new Object[]{
                    rs.getInt("member_id"), rs.getString("name"), rs.getString("email"),
                    rs.getString("phone"), rs.getString("membership_type")
                });
            }
            rs.close(); stmt.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadReturnDropdown(JComboBox<String> returnTxnCombo) {
        returnTxnCombo.removeAllItems();
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            Statement s = con.createStatement();
            String activeTxnQuery = "SELECT t.transaction_id, b.title, m.name FROM transactions t " +
                    "JOIN books b ON t.book_id = b.book_id JOIN members m ON t.member_id = m.member_id " +
                    "WHERE t.status = 'ISSUED'";
            ResultSet rsTxns = s.executeQuery(activeTxnQuery);
            while (rsTxns.next()) {
                returnTxnCombo.addItem(rsTxns.getInt("transaction_id") + " | " + rsTxns.getString("title") + " (" + rsTxns.getString("name") + ")");
            }
            rsTxns.close(); s.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadLedger(DefaultTableModel ledgerModel) {
        ledgerModel.setRowCount(0);
        String query = "SELECT t.transaction_id, b.title, m.name, t.issue_date, t.due_date, t.return_date, t.fine_amount, t.status " +
                "FROM transactions t JOIN books b ON t.book_id = b.book_id " +
                "JOIN members m ON t.member_id = m.member_id";
        try {
            Connection con = DriverManager.getConnection(URL, USER, PASS);
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery(query);
            while (rs.next()) {
                String returnDate = rs.getString("return_date");
                if (returnDate == null) returnDate = "Not Returned";
                ledgerModel.addRow(new Object[]{
                    rs.getInt("transaction_id"), rs.getString("title"), rs.getString("name"),
                    rs.getString("issue_date"), rs.getString("due_date"), returnDate,
                    rs.getDouble("fine_amount"), rs.getString("status")
                });
            }
            rs.close(); s.close(); con.close();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}