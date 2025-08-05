 Arimaa Board Game – Kotlin Jetpack Compose

This is a mobile board game app built using Kotlin and Jetpack Compose, recreating the strategic game of Arimaa for two players. The UI is fully custom-drawn in a single composable, respecting performance, touch input, and game logic integrity.

---

 Game Features

♟️ 8x8 Interactive Game Board
  - Custom-rendered board with trap squares
  - Fixed starting arrangement of animal pieces

  🐇 Two-Player Turn-Based Logic
  - Gold vs Silver turns with visual indicators
  - Enforces Arimaa movement rules

  👆 Touch Input for Movement
  - Tap to select a piece
  - Tap again to move
  - Highlights valid moves

  🚫 Game Rule Enforcement
  - No backward movement for rabbits
  - Pieces immobilized by stronger opponents
  - Trap square logic with removal rules
  - Valid move history — no repeated board states

  🧠 Game Mechanics
  - Push/Pull logic using two-step moves
  - Win detection when a rabbit reaches the opponent's side
  - Undo moves before committing a turn

  📱 Modern Compose UI
  - Single composable handles board + rendering
  - Dynamic layout and intuitive interaction
  - Reset and feedback components for game clarity



 🛠️ Tech Stack

- Language: Kotlin
- UI Toolkit: Jetpack Compose
- State Management: Compose State + Custom Logic
- Persistence: In-memory (no database)
- Build System: Gradle (KTS)


💡 What I Focused On

- Writing **modular, clean, and readable** Kotlin code
- Handling **edge cases** for game state (immobility, trap rules, move limits)
- Designing an intuitive UI despite the **one-composable constraint**
- Managing **touch input** effectively
- Creating a fun, functional digital version of a unique strategy game

Project Structure
  app/
├── ui/
│ ├── BoardComposable.kt # Single composable for board
│ ├── PieceRenderer.kt # Renders animals
│ └── UIControls.kt # Turn indicator, messages, reset
├── model/
│ ├── Piece.kt # Game piece logic
│ ├── BoardState.kt # Board history, trap handling
├── GameViewModel.kt
├── MainActivity.kt

Open in Android Studio 
   File > Open > Navigate to project root

Run on Emulator or Device
   
