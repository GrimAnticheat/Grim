# Contributing to GrimAC

Thank you for your interest in contributing to GrimAC. This document outlines the guidelines for
making pull
requests to the project. *We're usually pretty lenient with pull requests, but this guide will help
make the process go more smoothly.*

### Pull Request Guidelines

- **Compatibility**
  - Any changes must be compatible with
  the [supported environments](https://github.com/GrimAnticheat/Grim/wiki/Supported-environments) (Spigot, Paper, Folia, Fabric, etc.)
  - The plugin must be compatible with minecraft versions 1.8 and higher. Exempting checks from specific versions is acceptable.
  - The plugin must be able to run on Java 17 or higher. Changes that don't support Java 17 at runtime will not be accepted.

- **Non-acceptable pull requests**
  - Heuristic-based checks will not be accepted; however, basic rate limiting such as blocking attacks based on CPS is acceptable.
  - Checks that can be easily circumvented that don't block packets or fix anything are likely to not be accepted.
  - Checks or features that are too specific to a single environment or minecraft version are likely to not be accepted.
  - Changes that require large or unnecessary dependencies will likely not be accepted.

- **Pull request formatting**
  - Create a new branch for your feature or fix when forking the repository.
  - Reference related issues in your pull request description if applicable.
  - Write clear and descriptive commit messages.

- **Code styling**
  - Add code comments for complex logic or significant changes.
  - Try to keep your code clean and avoid duplication.
  - Thoroughly test your changes before submitting your pull request.

### Development Notes

- GrimAC is built using [Gradle](https://gradle.org/) kotlin scripts.
- Java 21 is currently required to build the project. A minimum of Java 17 is required to run it.

### Questions & Support

- Join our [Discord](https://discord.grim.ac) if you have questions or need assistance.
- Refer to the [Wiki](https://github.com/GrimAnticheat/Grim/wiki) for project documentation.
