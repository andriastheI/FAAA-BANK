import java.awt.CardLayout
import java.awt.Font
import java.io.File
import javax.swing.*
import kotlin.system.exitProcess

/**
 * Filename: Main.kt
 * Author: Andrias Zelele
 * Date: 2026-02-22
 *
 * Description:
 * The main entry point for the FAAFO BANK desktop application. This class is responsible for:
 * - Loading and saving customer account data to/from a local storage file
 * - Managing the in-memory account list and the currently authenticated user
 * - Bootstrapping the Swing UI and configuring a CardLayout-based navigation system
 *
 * Notes:
 * - Account storage format (per line): name|age|id|loan|pbkdf2String
 * - The PBKDF2 stored string may contain ':' internally and is preserved as-is.
 */
class Main {

    /** File path where account data is stored. */
    private val fileName = "data/accountstorage.txt"

    /** In-memory list of all user accounts loaded from storage. */
    private var userAccounts = mutableListOf<Customer>()

    /** Currently logged-in user (defaults to an empty Customer instance). */
    private var theUser: Customer = Customer()

    /** Field delimiter used to serialize/deserialize account records. */
    private val delim = "|"

    /**
     * Loads all customer accounts from the storage file into memory.
     *
     * Behavior:
     * - If the file does not exist, the method exits silently.
     * - Clears the current in-memory list before loading fresh data.
     * - Skips blank or malformed lines safely.
     */
    fun loadFile() {
        val file = File(fileName)
        if (!file.exists()) return

        // Ensure we do not keep stale in-memory data before loading.
        userAccounts.clear()

        file.readLines().forEach { line ->
            // Skip empty lines to avoid parse errors.
            if (line.isBlank()) return@forEach

            // Expected format:
            // name|age|id|loan|pbkdf2String (pbkdf2String contains ':' internally)
            val parts = line.split(delim).map { it.trim() }
            if (parts.size < 5) return@forEach

            // Parse base fields; if any are invalid, skip this record.
            val name = parts[0]
            val age = parts[1].toIntOrNull() ?: return@forEach
            val id = parts[2].toIntOrNull() ?: return@forEach
            val loan = parts[3].toIntOrNull() ?: return@forEach

            // PBKDF2 stored string: iterations:saltB64:hashB64
            // Join the remainder of the record to preserve the full hash value.
            val storedHash = parts.subList(4, parts.size).joinToString(delim).trim()
            // (subList join is just extra-safe in case name ever contains '|', but mainly keeps it robust)

            // Create a Customer model from the parsed fields and add it to memory.
            val customer = Customer(name, age, id, loan, storedHash)
            userAccounts.add(customer)
        }
    }

    /**
     * Saves all users currently in memory to the storage file.
     *
     * Behavior:
     * - Creates parent directories if needed.
     * - Overwrites the file each time to reflect the current in-memory state.
     *
     * @return true if saving succeeds; false if an exception occurs.
     */
    fun saveAllUsers(): Boolean {
        return try {
            val file = File(fileName)

            // Ensure the "data/" directory exists before writing the file.
            file.parentFile?.mkdirs()

            // Serialize each customer using the same delimiter format used during load.
            val content = userAccounts.joinToString("\n") { c ->
                "${c.getName()}$delim${c.getAge()}$delim${c.getId()}$delim${c.getCurrentLoan()}$delim${c.getHashedPassword()}"
            }

            // Persist the serialized content to disk.
            file.writeText(content)
            true
        } catch (e: Exception) {
            // Log error details for debugging; return false to indicate failure.
            println("Error saving file: ${e.message}")
            false
        }
    }

    /**
     * Attempts to withdraw a specified amount from the current user's available loan.
     *
     * @param amount the amount to withdraw
     * @return true if the user's loan is updated successfully; false otherwise
     */
    fun takeWithdrawal(amount: Int): Boolean {
        val newAmount = theUser.getCurrentLoan() - amount
        return theUser.setCurrentLoan(newAmount)
    }

    /**
     * Checks whether a username already exists in the in-memory account list.
     *
     * @param name the username to search for
     * @return true if the username exists; false otherwise
     */
    fun checkUserNameExist(name: String): Boolean {
        for (user in userAccounts) {
            if (user.getName() == name) return true
        }
        return false
    }

    companion object {
        /**
         * Application entry point. Initializes the Swing UI on the Event Dispatch Thread (EDT).
         *
         * @param args command-line arguments (not used)
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Ensure Swing components are created and updated on the EDT.
            SwingUtilities.invokeLater {
                val app = Main()

                // Main application window setup.
                val frame = JFrame("FAAFO BANK")
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                frame.setSize(500, 400)
                frame.setLocationRelativeTo(null)

                // CardLayout enables swapping between different screens (panels).
                val cardLayout = CardLayout()
                val cards = JPanel(cardLayout)

                // -------------------------
                // HOME CARD (main menu)
                // -------------------------
                val homePanel = JPanel().apply { layout = null }

                // Application title label.
                val titleLabel = JLabel("FAAFO BANK").apply {
                    font = font.deriveFont(32f)
                    setBounds(140, 20, 240, 40)
                }
                homePanel.add(titleLabel)

                // Navigates to the Sign In flow.
                val signInButton = JButton("Sign In").apply {
                    font = font.deriveFont(20f)
                    setBounds(160, 140, 160, 30)
                }
                homePanel.add(signInButton)

                // Navigates to the Sign Up flow.
                val signUpButton = JButton("Sign Up").apply {
                    font = font.deriveFont(20f)
                    setBounds(160, 180, 160, 30)
                }
                homePanel.add(signUpButton)

                // Exits the application cleanly.
                val exitButton = JButton("Exit").apply {
                    font = font.deriveFont(20f)
                    setBounds(190, 220, 100, 30)
                    addActionListener { frame.dispose(); exitProcess(0) }
                }
                homePanel.add(exitButton)
                // -------------------------
                // SIGN IN CARD
                // -------------------------
                // Screen that collects username + password and routes the user to the authenticated menu.
                val signInPanel = JPanel().apply { layout = null }

                // Page title label.
                val signInTitle = JLabel("Sign In").apply {
                    font = font.deriveFont(28f)
                    setBounds(190, 20, 200, 40)
                }
                signInPanel.add(signInTitle)

                // Username label + input field.
                val usernameLabel = JLabel("Username: ").apply {
                    font = font.deriveFont(16f)
                    setBounds(100, 120, 100, 25)
                }
                val usernameInput = JTextField(15).apply {
                    font = font.deriveFont(16f)
                    setBounds(210, 120, 120, 25)
                }
                signInPanel.add(usernameLabel)
                signInPanel.add(usernameInput)

                // Password label + password input field (masked).
                val userPasswordLabel = JLabel("Password: ").apply {
                    font = font.deriveFont(16f)
                    setBounds(100, 160, 100, 25)
                }
                val userPasswordInput = JPasswordField().apply {
                    font = font.deriveFont(16f)
                    setBounds(210, 160, 120, 25)
                }
                signInPanel.add(userPasswordLabel)
                signInPanel.add(userPasswordInput)

                // Attempts authentication using the provided credentials.
                val loginButton = JButton("Login In").apply {
                    font = font.deriveFont(20f)
                    setBounds(160, 230, 120, 25)
                }
                signInPanel.add(loginButton)

                // Returns the user to the Home screen.
                val backFromSignIn = JButton("Back").apply {
                    setBounds(20, 20, 80, 30)
                }
                signInPanel.add(backFromSignIn)

                // -------------------------
                // LOGIN CARD
                // -------------------------
                // Authenticated menu screen shown after a successful sign-in.
                val loginPanel = JPanel().apply { layout = null }

                // Dynamic title (typically displays a welcome message or the current user's name).
                val loginTitle = JLabel().apply {
                    font = font.deriveFont(Font.BOLD, 20f)
                    setBounds(40, 20, 250, 40)
                }
                loginPanel.add(loginTitle)

                // Routes to the account balance view.
                val checkBalanceButton = JButton("Check Balance").apply {
                    font = font.deriveFont(16f)
                    setBounds(140, 100, 240, 30)
                }

                // Routes to the withdrawal workflow.
                val withDButton = JButton("Withdrawal").apply {
                    font = font.deriveFont(16f)
                    setBounds(140, 140, 240, 30)
                }

                // Routes to the password update workflow.
                val changePasswordButton = JButton("Change Password").apply {
                    font = font.deriveFont(16f)
                    setBounds(140, 180, 240, 30)
                }

                // Logs the current user out and returns to the Home screen.
                val logOutButton = JButton("Log Out").apply {
                    font = font.deriveFont(16f)
                    setBounds(320, 300, 140, 30)
                }

                // Register buttons on the authenticated menu screen.
                loginPanel.add(checkBalanceButton)
                loginPanel.add(withDButton)
                loginPanel.add(changePasswordButton)
                loginPanel.add(logOutButton)

                // -------------------------
                // CHECK BALANCE CARD
                // -------------------------
                // Screen responsible for displaying the currently logged-in user's balance/loan details.
                val checkBalancePanel = JPanel().apply { layout = null }

                // Page title label.
                val checkBalanceTitle = JLabel("CHECK BALANCE").apply {
                    font = font.deriveFont(Font.BOLD, 20f)
                    setBounds(80, 50, 250, 40)
                }
                // Dynamic label used to display the current user's loan/balance information.
                val currentLoanLabel = JLabel().apply {
                    font = font.deriveFont(16f)
                    setBounds(70, 180, 300, 40)
                }

                // Register the title and dynamic balance label on the Check Balance screen.
                checkBalancePanel.add(checkBalanceTitle)
                checkBalancePanel.add(currentLoanLabel)

                // Returns the user to the authenticated menu (Login card).
                val backFromCheckBalance= JButton("Back").apply {
                    setBounds(20, 20, 80, 30)
                }
                checkBalancePanel.add(backFromCheckBalance)

                // -------------------------
                // WITHDRAWAL CARD
                // -------------------------
                // Screen that allows the authenticated user to request a withdrawal amount.
                val withdrawalPanel = JPanel().apply { layout = null }

                // Dynamic label used to display the user's current withdrawal limit / available loan.
                val withdrawalLimitLabel = JLabel().apply {
                    setBounds(400, 20, 140, 25)
                }

                // Page title label.
                val withdrawalTitle = JLabel("WITHDRAWAL").apply {
                    font = font.deriveFont(Font.BOLD, 20f)
                    setBounds(80, 50, 250, 40)
                }
                withdrawalPanel.add(withdrawalTitle)
                withdrawalPanel.add(withdrawalLimitLabel)

                // Withdrawal amount prompt + input field.
                val withdrawalLabel = JLabel("Withdrawal Amount $").apply {
                    setBounds(40, 120, 140, 25)
                }
                val withdrawalAmountIn = JTextField(15).apply {
                    setBounds(180, 120, 60, 25)
                }
                withdrawalPanel.add(withdrawalLabel)
                withdrawalPanel.add(withdrawalAmountIn)

                // Submits the withdrawal request.
                val withdrawalButton = JButton("Withdraw").apply {
                    font = font.deriveFont(16f)
                    setBounds(320, 300, 140, 30)
                }
                withdrawalPanel.add(withdrawalButton)

                // Returns the user to the authenticated menu (Login card).
                val backFromWithdrawal= JButton("Back").apply {
                    setBounds(20, 20, 80, 30)
                }
                withdrawalPanel.add(backFromWithdrawal)

                // -------------------------
                // CHANGE PASSWORD CARD
                // -------------------------
                // Screen that allows the authenticated user to change their account password.
                val changePasswordPanel = JPanel().apply { layout = null }

                // Page title label.
                val changePasswordTitleLabel = JLabel("CHANGE PASSWORD").apply {
                    font = font.deriveFont(Font.BOLD, 20f)
                    setBounds(80, 50, 120, 30)
                }

                // Current password prompt + input field.
                val currentPasswordLabel = JLabel("Current Password").apply {
                    setBounds(40, 140, 120, 25)
                }
                val currentPasswordInput = JPasswordField(15).apply {
                    setBounds(160, 140, 200, 25)
                }

                // New password prompt + input field.
                val newPasswordLabel = JLabel("New Password").apply {
                    setBounds(40, 180, 120, 25)
                }
                val newPasswordInput = JPasswordField(15).apply {
                    setBounds(160, 180, 200, 25)
                }

                // Confirm new password prompt + input field.
                val confirmPasswordLabel = JLabel("Confirm Password").apply {
                    setBounds(40, 220, 120, 25)
                }
                val confirmPasswordInput = JPasswordField(15).apply {
                    setBounds(160, 220, 200, 25)
                }

                // Password policy reminder for the user.
                val passwordChangeInfoLabel = JLabel("* must be 8 Characters(no Space) or more").apply {
                    setBounds(160, 238, 250, 25)
                }

                // Register all Change Password UI components.
                changePasswordPanel.add(currentPasswordLabel)
                changePasswordPanel.add(changePasswordTitleLabel)
                changePasswordPanel.add(currentPasswordInput)
                changePasswordPanel.add(newPasswordLabel)
                changePasswordPanel.add(newPasswordInput)
                changePasswordPanel.add(confirmPasswordLabel)
                changePasswordPanel.add(confirmPasswordInput)
                changePasswordPanel.add(passwordChangeInfoLabel)

                // Confirms and submits the password change request.
                val confirmPasswordButton = JButton("Confirm").apply {
                    font = font.deriveFont(16f)
                    setBounds(320, 300, 140, 30)
                }
                changePasswordPanel.add(confirmPasswordButton)

                // Returns the user to the authenticated menu (Login card).
                val backFromChangePassword= JButton("Back").apply {
                    setBounds(20, 20, 80, 30)
                }
                changePasswordPanel.add(backFromChangePassword)
                // -------------------------
                // SIGN UP CARD
                // -------------------------
                // Screen that collects new user information and creates a new customer account.
                val signUpPanel = JPanel().apply { layout = null }

                // Page title label.
                val signUpTitle = JLabel("Sign Up").apply {
                    font = font.deriveFont(28f)
                    setBounds(185, 20, 200, 40)
                }
                signUpPanel.add(signUpTitle)

                // Username prompt + input field.
                val userFnameLabelUp = JLabel("Username:").apply {
                    setBounds(90, 120, 80, 25)
                }
                val userFnameInputUp = JTextField(15).apply {
                    setBounds(160, 120, 200, 25)
                }

                // Username policy reminder.
                val usernameInfoLabelUp = JLabel("* must be 6 characters(only letters) or more").apply {
                    setBounds(160, 138, 250, 25)
                }

                // Age prompt + input field.
                val userAgeLabelUp = JLabel("Age:").apply {
                    setBounds(90, 160, 80, 25)
                }
                val userAgeInputUp = JTextField(15).apply {
                    setBounds(160, 160, 200, 25)
                }

                // Age requirement reminder.
                val ageInfoLabelUp = JLabel("* must be above 18").apply {
                    setBounds(160, 178, 250, 25)
                }

                // Student ID prompt + input field.
                val userIdLabelUp = JLabel("Student ID:").apply {
                    setBounds(90, 200, 80, 25)
                }
                val userIdInputUp = JTextField(15).apply {
                    setBounds(160, 200, 200, 25)
                }

                // Student ID format reminder.
                val idInfoLabelUp = JLabel("* must be 7 Characters").apply {
                    setBounds(160, 218, 250, 25)
                }

                // Password prompt + input field (masked).
                val userPasswordLabelUp = JLabel("Password:").apply {
                    setBounds(90, 240, 80, 25)
                }
                val userPasswordInputUp = JPasswordField(15).apply {
                    setBounds(160, 240, 200, 25)
                }

                // Password policy reminder.
                val passwordInfoLabelUp = JLabel("* must be 8 Characters(no Space) or more").apply {
                    setBounds(160, 258, 250, 25)
                }

                // Register all Sign Up UI components.
                signUpPanel.add(userFnameLabelUp)
                signUpPanel.add(userFnameInputUp)
                signUpPanel.add(userAgeLabelUp)
                signUpPanel.add(userAgeInputUp)
                signUpPanel.add(userIdLabelUp)
                signUpPanel.add(userIdInputUp)
                signUpPanel.add(userPasswordLabelUp)
                signUpPanel.add(userPasswordInputUp)
                signUpPanel.add(passwordInfoLabelUp)
                signUpPanel.add(idInfoLabelUp)
                signUpPanel.add(ageInfoLabelUp)
                signUpPanel.add(usernameInfoLabelUp)

                // Returns the user to the Home screen without creating an account.
                val backFromSignUp = JButton("Back").apply {
                    setBounds(20, 20, 80, 30)
                }

                // Submits the Sign Up form and attempts to create a new account.
                val createAccountUp = JButton("Create Account").apply {
                    setBounds(320, 330, 150, 30)
                }

                // Register action buttons on the Sign Up screen.
                signUpPanel.add(createAccountUp)
                signUpPanel.add(backFromSignUp)

                // -------------------------
                // Add cards + wire buttons
                // -------------------------
                // Register each panel with the CardLayout using a unique string key.
                cards.add(homePanel, "HOME")
                cards.add(signInPanel, "SIGN_IN")
                cards.add(signUpPanel, "SIGN_UP")
                cards.add(loginPanel, "LOGIN")
                cards.add(checkBalancePanel, "CHECK_BALANCE")
                cards.add(withdrawalPanel, "WITHDRAWAL")
                cards.add(changePasswordPanel, "CHANGE_PASSWORD")

                // Navigation: Home -> Sign In.
                signInButton.addActionListener { cardLayout.show(cards, "SIGN_IN") }

                // Navigation: Home -> Sign Up.
                signUpButton.addActionListener { cardLayout.show(cards, "SIGN_UP") }

                // Navigation: Sign In -> Home (also clears sensitive input fields).
                backFromSignIn.addActionListener {
                    usernameInput.text = ""
                    userPasswordInput.text = ""
                    cardLayout.show(cards, "HOME")
                }

                // Navigation: Sign Up -> Home.
                backFromSignUp.addActionListener { cardLayout.show(cards, "HOME") }

                // Attempts to authenticate the user and route to the authenticated menu.
                loginButton.addActionListener {

                    // Load users from file before attempting sign-in.
                    app.loadFile()

                    // Collect user-entered credentials.
                    val username = usernameInput.text.trim()
                    val password = String(userPasswordInput.password)

                    // Locate an account by username.
                    val foundUser = app.userAccounts.find { it.getName() == username }

                    // Verify password and proceed to authenticated menu on success.
                    if (foundUser != null && foundUser.verifyPassword(password)) {

                        // Store the authenticated user in memory for subsequent actions.
                        app.theUser = foundUser

                        // Update the login title dynamically (e.g., a welcome message).
                        loginTitle.text = "Welcome ${foundUser.getName()}"

                        // Switch to authenticated menu card.
                        cardLayout.show(cards, "LOGIN")

                        // Clear username and password fields after login for security.
                        usernameInput.text = ""
                        userPasswordInput.password.fill('\u0000')
                        userPasswordInput.text = ""

                    } else {
                        // Show a user-friendly error message for failed authentication.
                        JOptionPane.showMessageDialog(
                            frame,
                            "Invalid username or password",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
                // Handles account creation from the Sign Up form.
                createAccountUp.addActionListener {
                    // Load existing users from storage to ensure we validate against the latest data.
                    app.loadFile()

                    // Create a new Customer object to populate/validate before saving.
                    val newCustomer = Customer()

                    // Collect raw input values from the Sign Up form.
                    val userInName = userFnameInputUp.text.trim()
                    val ageText = userAgeInputUp.text.trim()
                    val userIdText = userIdInputUp.text.trim()
                    val passwordChars = userPasswordInputUp.password

                    // Convert numeric fields (age and student ID) safely.
                    val age = ageText.toIntOrNull()
                    val userId = userIdText.toIntOrNull()

                    // ---- VALIDATION ----
                    // Enforce unique usernames before applying any other validation rules.
                    if (app.checkUserNameExist(userInName)){
                        JOptionPane.showMessageDialog(
                            frame,
                            "Username already Taken!!",
                            "Account Creation Failed",
                            JOptionPane.ERROR_MESSAGE
                        )
                        return@addActionListener
                    }

                    // Validate and assign each field using the Customer model's validation rules.
                    if (!newCustomer.setName(userInName)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Username Input is Invalid",
                            "Account Creation Failed",
                            JOptionPane.ERROR_MESSAGE
                        )

                    } else if (age == null || !newCustomer.setAge(age)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Age Input is Invalid",
                            "Account Creation Failed",
                            JOptionPane.ERROR_MESSAGE
                        )

                    } else if (userId == null || !newCustomer.setId(userId)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "ID Input is Invalid",
                            "Account Creation Failed",
                            JOptionPane.ERROR_MESSAGE
                        )

                    } else if (!newCustomer.setPassword(passwordChars)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Password Input is Invalid",
                            "Account Creation Failed",
                            JOptionPane.ERROR_MESSAGE
                        )

                    } else {
                        // Assign default starting loan limit for newly created accounts.
                        newCustomer.setCurrentLoan(100)

                        // Add the validated user to the in-memory list.
                        app.userAccounts.add(newCustomer)

                        // Persist all users to storage (file is overwritten by design).
                        val saved = app.saveAllUsers()

                        if (saved) {
                            // Notify the user that the account creation was successful.
                            JOptionPane.showMessageDialog(
                                frame,
                                "Account Created Successfully (You can sign in now)",
                                "Account Created",
                                JOptionPane.INFORMATION_MESSAGE
                            )

                            // Clear all Sign Up fields and wipe password chars for safety.
                            userFnameInputUp.text = ""
                            userAgeInputUp.text = ""
                            userIdInputUp.text = ""
                            passwordChars.fill('\u0000')
                            userPasswordInputUp.text = ""

                        } else {
                            // Notify the user if persistence fails.
                            JOptionPane.showMessageDialog(
                                frame,
                                "Failed saving the account",
                                "Account Creation Failed",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                }

                // Routes the authenticated user to the Check Balance screen and refreshes the displayed value.
                checkBalanceButton.addActionListener {
                    // Update the balance label dynamically using the currently authenticated user.
                    currentLoanLabel.text = "Your current loan limit is $${app.theUser.getCurrentLoan()}"

                    // Switch to the Check Balance card.
                    cardLayout.show(cards, "CHECK_BALANCE")
                }

                // Navigation: Check Balance -> Authenticated menu.
                backFromCheckBalance.addActionListener { cardLayout.show(cards, "LOGIN") }

                // Routes the authenticated user to the Withdrawal screen and refreshes the displayed limit.
                withDButton.addActionListener {
                    // Display the current available loan limit prior to withdrawal.
                    withdrawalLimitLabel.text = "loan limit: $${app.theUser.getCurrentLoan()}"

                    // Switch to the Withdrawal card.
                    cardLayout.show(cards, "WITHDRAWAL")
                }

                // Processes a withdrawal request after validating input and confirming user intent.
                withdrawalButton.addActionListener {
                    // Read and validate the withdrawal amount from the input field.
                    val loanAmountText = withdrawalAmountIn.text.trim()
                    val loanAmount = loanAmountText.toIntOrNull()

                    // Reject invalid values (non-numeric, non-positive, or exceeding the current limit).
                    if (loanAmount == null || loanAmount <= 0 || loanAmount > app.theUser.getCurrentLoan()) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Loan amount is Invalid!",
                            "Loan Processing Failed",
                            JOptionPane.ERROR_MESSAGE
                        )
                    } else {
                        // Confirm the action with the user before applying changes.
                        val result = JOptionPane.showConfirmDialog(
                            frame,
                            "Are you sure you want to take out $${loanAmountText}?",
                            "Confirm Action",
                            JOptionPane.YES_NO_OPTION
                        )

                        if (result == JOptionPane.YES_OPTION) {
                            // Apply the withdrawal and persist changes.
                            val check = app.takeWithdrawal(loanAmount)
                            val saved = app.saveAllUsers()

                            if (check && saved) {
                                // Notify the user of success and return to the authenticated menu.
                                JOptionPane.showMessageDialog(
                                    frame,
                                    "Withdrawal successfully",
                                    "Withdrawal taken",
                                    JOptionPane.INFORMATION_MESSAGE
                                )
                                withdrawalAmountIn.text = ""
                                cardLayout.show(cards, "LOGIN")
                            } else {
                                // Notify the user if business logic or persistence fails.
                                JOptionPane.showMessageDialog(
                                    frame,
                                    "Withdrawal wasn't processed, Please contact tech support!",
                                    "Loan Processing Failed",
                                    JOptionPane.ERROR_MESSAGE
                                )
                            }
                        }
                    }
                }
                // Navigation: Withdrawal -> Authenticated menu.
                backFromWithdrawal.addActionListener { cardLayout.show(cards, "LOGIN") }

                // Navigation: Authenticated menu -> Change Password screen.
                changePasswordButton.addActionListener { cardLayout.show(cards, "CHANGE_PASSWORD") }

                // Handles password change workflow for the currently authenticated user.
                confirmPasswordButton.addActionListener {
                    // Capture password inputs as character arrays (preferred over String for security).
                    val oldPasswordChars = currentPasswordInput.password
                    val newPasswordChars = newPasswordInput.password
                    val confirmPasswordChars = confirmPasswordInput.password

                    // Verify that the current password is correct before allowing a change.
                    val currentOk = app.theUser.verifyPassword(oldPasswordChars)
                    if (!currentOk) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Current password is invalid!",
                            "Password Invalid",
                            JOptionPane.ERROR_MESSAGE
                        )
                        return@addActionListener
                    }

                    // Ensure the new password matches the confirmation field.
                    if (!newPasswordChars.contentEquals(confirmPasswordChars)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "New password does not match Confirm Password.",
                            "New Password Invalid",
                            JOptionPane.ERROR_MESSAGE
                        )
                        return@addActionListener
                    }

                    // Attempt to set the new password and persist changes to storage.
                    val changed = app.theUser.setPassword(confirmPasswordChars)
                    val saved = changed && app.saveAllUsers()

                    if (saved) {
                        // Notify the user that the password change succeeded.
                        JOptionPane.showMessageDialog(
                            frame,
                            "New password has been changed successfully.",
                            "Password Changed",
                            JOptionPane.INFORMATION_MESSAGE
                        )

                        // Clear UI fields after success.
                        currentPasswordInput.text = ""
                        newPasswordInput.text = ""
                        confirmPasswordInput.text = ""

                        // Wipe password arrays from memory after use.
                        oldPasswordChars.fill('\u0000')
                        newPasswordChars.fill('\u0000')
                        confirmPasswordChars.fill('\u0000')

                        // Return to the authenticated menu.
                        cardLayout.show(cards, "LOGIN")
                    } else {
                        // Notify the user if the update or persistence step fails.
                        JOptionPane.showMessageDialog(
                            frame,
                            "Password change wasn't processed. Please contact tech support!",
                            "Password Change Failed",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }

                // Navigation: Change Password -> Authenticated menu.
                backFromChangePassword.addActionListener { cardLayout.show(cards, "LOGIN") }

                // Logs the user out and returns to the Sign In screen.
                logOutButton.addActionListener {
                    cardLayout.show(cards, "SIGN_IN")
                }

                // Attach the CardLayout container to the frame and display the application window.
                frame.contentPane = cards
                frame.isVisible = true
            }
        }
    }
}