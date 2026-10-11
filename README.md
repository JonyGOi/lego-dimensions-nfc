# LEGO Dimensions NFC Writer 🎮📱

Um aplicativo Android nativo moderno em **Jetpack Compose** e **Kotlin** para leitura, geração e gravação de tags NFC (NTAG213 e NTAG215) para o jogo **LEGO Dimensions**.

Criado e desenvolvido por **João Pedro Favoni Fernandes** 🇧🇷.

---

## 🌟 Funcionalidades

- **Catálogo Completo**:
  - Todos os 75+ personagens do jogo com habilidades e franquias detalhadas.
  - Todos os veículos e gadgets em **todos os 3 níveis de reconstrução** (Nível 1 - Padrão, Nível 2 e Nível 3), totalizando mais de 260 variações.
  - Filtro prático por categoria e nível de reconstrução.
- **Ícones Originais Offline**:
  - Mais de 310 ícones circulares correspondentes aos tokens das etiquetas originais embutidos no aplicativo.
- **Gravação Ultra-Rápida (< 1 segundo)**:
  - Gravação otimizada com 3 comandos essenciais diretos:
    1. Página `0x24` (ID do personagem criptografado com a UID da tag ou ID do veículo)
    2. Página `0x25`/`0x26` (Chave de criptografia ou flag de veículo `00 01 00 00`)
    3. Página `0x2B`/`0x85` (Senha de proteção NTAG `PWD`)
- **Suporte a Multi-Línguas**:
  - Suporte completo a dezenas de idiomas (Português, Inglês, Espanhol, Francês, Alemão, Italiano, Japonês, Chinês e mais).
  - Seletor de idioma em tempo real dentro do app.
- **Modo Avançado**:
  - Gravação direta por ID numérico.
  - Suporte a tags NTAG213 e NTAG215 (incluindo tags com UID gravável / Gen2).
- **Histórico Local**:
  - Registro de todas as tags gravadas com data, status e UID.
- **Aba de Créditos**:
  - Informações do autor do projeto e agradecimentos à comunidade open-source.

---

## 🚀 Como Compilar e Executar

### Pré-requisitos
- Android Studio Iguana / Jellyfish ou superior
- JDK 17
- Android SDK 34
- Dispositivo Android com leitor NFC ativado (Android 8.0+)

### Compilação via Linha de Comando (Gradle)

```bash
# Clone o repositório
git clone https://github.com/SEU_USUARIO/lego-dimensions-nfc.git
cd lego-dimensions-nfc

# Compilar versão de depuração (Debug APK)
./gradlew assembleDebug

# O APK estará disponível em:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Kotlin 1.9.22
- **UI Toolkit**: Jetpack Compose com Material Design 3
- **NFC**: Android NFC Adapter (ReaderMode com `FLAG_READER_NFC_A`)
- **Criptografia**: Algoritmo Tiny Encryption Algorithm (TEA) customizado para LEGO Dimensions
- **Arquitetura**: MVVM com StateFlow e Coroutines

---

## 👨‍💻 Créditos e Agradecimentos

- **Desenvolvedor**: João Pedro Favoni Fernandes 🇧🇷
- **Agradecimentos Especiais**:
  - Comunidade de preservação e engenharia reversa do LEGO Dimensions.
  - Laurent Ellerbach pelo repositório aberto de documentação e algoritmos de criptografia de tags do LEGO Dimensions.
  - Ferramentas comunitárias de etiquetas e tokens.

---

## 📄 Licença

Este projeto é destinado para fins educacionais e de preservação. LEGO® e LEGO Dimensions são marcas registradas do Grupo LEGO.
