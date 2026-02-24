import kotlin.math.absoluteValue

/**
 * Filename: Customer.kt
 * Author: Andrias Zelele
 * Date: 2026-02-22
 *
 * Description:
 * Represents a FAAFO BANK customer account. This class encapsulates all
 * customer-related data and validation logic including:
 * - Username, age, student ID, and loan limit management
 * - Password hashing and verification using PBKDF2 via PasswordHash
 * - Input validation rules for secure and consistent account handling
 *
 * Notes:
 * - Passwords are never stored in plain text.
 * - The stored password format is: iterations:saltB64:hashB64.
 */

/**
 * Models a customer account and provides validation, loan updates, and
 * password hashing/verification logic (PBKDF2 via PasswordHash).
 *
 * @property username the customer's username
 * @property age the customer's age (must be 18 or older)
 * @property id the customer's student ID (must be 7 digits)
 * @property currentLoan the customer's current available loan amount
 * @property hashedPassword the stored password hash string (iterations:saltB64:hashB64)
 */
class Customer(
    private var username: String,
    private var age: Int,
    private var id: Int,
    private var currentLoan: Int,
    private var hashedPassword: String = ""
) {

    /** Default constructor used when creating a blank Customer before assigning values. */
    constructor() : this("", 0, 0, 0, "")

    /** Returns the username of this customer. */
    fun getName(): String = username

    /** Returns the customer's age. */
    fun getAge(): Int = age

    /** Returns the customer's student ID. */
    fun getId(): Int = id

    /** Returns the current available loan limit. */
    fun getCurrentLoan(): Int = currentLoan

    /** Returns the stored hashed password string. */
    fun getHashedPassword(): String = hashedPassword

    /**
     * Updates the current loan amount if the value is non-negative.
     *
     * @param newLoan the new loan amount
     * @return true if updated successfully; false otherwise
     */
    fun setCurrentLoan(newLoan: Int): Boolean {
        if (newLoan >= 0) {
            currentLoan = newLoan
            return true
        }
        return false
    }

    /**
     * Sets the username after validating format rules.
     * Username must be letters only and not blank.
     *
     * @param newName proposed username
     * @return true if valid and applied; false otherwise
     */
    fun setName(newName: String?): Boolean {
        if (!newName.isNullOrBlank() && checkName(newName)) {
            username = newName
            return true
        }
        return false
    }

    /**
     * Sets the customer's age.
     * Must be 18 or older.
     *
     * @param newAge age value
     * @return true if valid and applied; false otherwise
     */
    fun setAge(newAge: Int): Boolean {
        if (newAge >= 18) {
            age = newAge
            return true
        }
        return false
    }

    /**
     * Sets the student ID after validating that it is exactly 7 digits.
     *
     * @param id proposed student ID
     * @return true if valid and applied; false otherwise
     */
    fun setId(id: Int): Boolean {
        if (checkId(id)) {
            this.id = id
            return true
        }
        return false
    }

    /**
     * Hashes and stores the password using PasswordHash.hash().
     *
     * Security Notes:
     * - Raw password characters are validated before hashing.
     * - The hashing implementation is handled by PasswordHash.
     *
     * @param plainPassword the raw password characters
     * @return true if stored successfully; false if validation fails
     */
    fun setPassword(plainPassword: CharArray): Boolean {
        if (!isValidPassword(plainPassword)) {
            return false
        }

        hashedPassword = PasswordHash.hash(plainPassword)
        return true
    }

    /**
     * Verifies a password against the stored hash using PasswordHash.verify().
     *
     * Behavior:
     * - Clears the password char array as a best-effort security practice.
     * - Returns false if the stored hash is invalid or blank.
     *
     * @param plainPassword the raw password characters
     * @return true if correct; false otherwise
     */
    fun verifyPassword(plainPassword: CharArray): Boolean {
        if (hashedPassword.isBlank()) {
            plainPassword.fill('\u0000')
            return false
        }

        return try {
            PasswordHash.verify(plainPassword, hashedPassword) // clears array too (best-effort)
        } catch (e: IllegalArgumentException) {
            // Stored hash format is invalid or corrupted.
            plainPassword.fill('\u0000')
            false
        }
    }

    /**
     * Convenience overload used by UI code that provides a String password.
     * Internally converts to a CharArray for secure verification.
     *
     * @param plainPassword the raw password string
     * @return true if verified successfully; false otherwise
     */
    fun verifyPassword(plainPassword: String): Boolean {
        val chars = plainPassword.toCharArray()
        val ok = verifyPassword(chars)
        chars.fill('\u0000')
        return ok
    }

    /**
     * Username validation rule.
     * Username must contain letters only (no digits or symbols).
     *
     * @param inName username string to validate
     * @return true if valid; false otherwise
     */
    fun checkName(inName: String): Boolean {
        // Only alphabetic characters allowed.
        return inName.matches(Regex("^[A-Za-z]+$")) && inName.length >=6
    }

    /**
     * Validates that the given student ID is exactly 7 digits.
     * The absolute value is used to ignore negative signs.
     *
     * @param givenId student ID value
     * @return true if valid length; false otherwise
     */
    fun checkId(givenId: Int): Boolean {
        val length = givenId.absoluteValue.toString().length
        return length == 7
    }

    /**
     * Internal password validation rules.
     *
     * Requirements:
     * - Minimum length: 8
     * - Maximum length: 50
     * - No spaces allowed
     *
     * @param pw password characters
     * @return true if valid; false otherwise
     */
    private fun isValidPassword(pw: CharArray): Boolean {
        if (pw.isEmpty()) return false
        if (pw.size !in 8..50) return false
        if (pw.any { it == ' ' }) return false
        return true
    }
}