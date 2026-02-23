# FAAALOANBANK 💳

## Overview

FAAALOANBANK is a secure Kotlin desktop banking simulator built using
Kotlin and Java Swing. The application allows users to create accounts,
securely authenticate, check balances, withdraw funds, and update
passwords using PBKDF2 password hashing.

This project demonstrates:

-   Kotlin OOP design
-   Secure password hashing (PBKDF2WithHmacSHA256)
-   File-based persistence
-   Swing GUI with CardLayout navigation
-   Secure handling of CharArray passwords

------------------------------------------------------------------------

## Requirements

Before running this project, make sure you have the following installed:

### ✅ Java JDK (17 or newer)

Check installation:

    java -version

### ✅ Kotlin Compiler (CLI)

Check installation:

    kotlinc -version

------------------------------------------------------------------------

## Project Structure

    FAAALOANBANK/
    │
    ├── src/
    │   ├── Main.kt
    │   ├── Customer.kt
    │   └── PasswordHash.kt
    │
    ├── data/
    │   └── accountstorage.txt   (auto-created)
    │
    └── README.md

------------------------------------------------------------------------

## ⚠️ Important Note About the `data/` Folder

This application uses a **relative file path**:

    data/accountstorage.txt

The file location depends on the **working directory**:

-   Running from IntelliJ → `data/` is created in the project root ✅
    (recommended)
-   Running from inside `src/` in terminal → `src/data/` may be created
    ❌

👉 Always run the program from the **project root folder** to keep the
project structure clean.

------------------------------------------------------------------------

## ▶️ How to Run From Terminal (Recommended)

Open a terminal and navigate to the project root:

    cd "/home/dre/CS-495/Code/Projects/Kotlin/FAAA BANK"

### Step 1 --- Compile

    kotlinc src/Main.kt src/Customer.kt src/PasswordHash.kt -d out

### Step 2 --- Run

    kotlin -classpath out Main

The FAAALOANBANK GUI should launch.

This method ensures the `data/` folder is created in the correct
location.

------------------------------------------------------------------------

## ▶️ Running From IntelliJ

No special setup required. IntelliJ already uses the project root as the
working directory, so the `data/accountstorage.txt` file will be created
correctly.

------------------------------------------------------------------------

## Security Notes

-   Passwords are never stored in plain text.
-   PBKDF2WithHmacSHA256 hashing is used with a random salt.
-   Passwords are handled using `CharArray` and cleared from memory when
    possible.
-   Constant-time comparison helps reduce timing attacks.

------------------------------------------------------------------------

## Author

**Andrias Zelele**\
Computer Science / Data Science\
Carroll College

------------------------------------------------------------------------

## Educational Purpose

This project is intended for educational and demonstration purposes
only.
