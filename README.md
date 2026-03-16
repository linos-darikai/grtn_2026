# Ghana Road Transport Network 2026

A JavaFX application for analyzing and visualizing road networks in Ghana, featuring shortest path algorithms, cost analysis, and interactive graph visualization.

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Running the Application

**Main Application (Interactive GUI):**
```bash
mvn clean javafx:run
```

**Complexity Analysis (Generate Performance Plots):**
```bash
mvn compile exec:java -Dexec.mainClass="ComplexityAnalysis"
```

**Run Tests:**
```bash
mvn test
```

## 📦 Dependencies

This project uses the following libraries:

- **JavaFX 21.0.5** - GUI framework for interactive visualization
  - `javafx-controls` - UI components
  - `javafx-graphics` - Graphics rendering
  - `javafx-fxml` - FXML support

- **JFreeChart 1.5.4** - Chart generation for complexity analysis plots

All dependencies are managed via Maven and will be automatically downloaded when you build the project.

## 🎯 Key Features

- **Interactive Graph Visualization** - Force-directed layout of road networks
- **Shortest Path Algorithms** - Dijkstra's algorithm for fastest routes
- **Top 3 Path Analysis** - Find and compare multiple route options by total cost
- **Cost Breakdown** - Fuel cost (GHS/km) + Time cost (GHS/min)
- **Route Recommendation** - Compare fastest vs. cheapest routes
- **Complexity Analysis** - Performance testing and prediction up to 5000 nodes

## 📁 Data Format

The application accepts `.csv` or `.txt` files with the following format:

**CSV Format** (with header):
```csv
source,destination,distance_km,avg_time_min
Accra,Kumasi,250,180
Kumasi,Tamale,380,240
```

**TXT Format** (no header):
```
Accra,Kumasi,250,180
Kumasi,Tamale,380,240
```

## 📚 Documentation

### Full Documentation
For comprehensive documentation including architecture, algorithms, and implementation details, see:
- **[Project Documentation (Word)]** *(Add your Word document link here)*

### JavaDoc API Documentation

Generate JavaDoc documentation:
```bash
mvn javadoc:javadoc
```

View the generated JavaDocs:
```bash
open target/site/apidoc/index.html
```

Or on Windows:
```bash
start target/site/apidoc/index.html
```

### UML Diagrams
Class diagrams and use case diagrams are available in PlantUML format:
- [docs/class-diagram.puml](docs/class-diagram.puml)
- [docs/usecase-diagram.puml](docs/usecase-diagram.puml)

## 🏗️ Project Structure

```
src/main/java/
├── App.java                    - Application entry point
├── Ghana.java                  - Core graph algorithms and data structures
├── Town.java                   - Town/node representation
├── PathResult.java             - Shortest path result
├── PathWithCost.java           - Path with cost breakdown
├── RouteComparison.java        - Route comparison data
├── LoadScreen.java             - File loading screen
├── MainScreen.java             - Main UI and visualization
└── ComplexityAnalysis.java     - Performance testing and plotting
```

## 🔬 Complexity Analysis

The `ComplexityAnalysis` class measures and predicts algorithm performance:

- **Measures** actual performance from 10 to 900 nodes
- **Predicts** scaling behavior from 900 to 5000 nodes
- **Generates** interactive plots with JFreeChart
- **Analyzes** three algorithms:
  - `getFastestTime()` - O(V log V)
  - `getTop3PathsByTotalCost()` - O(V²)
  - `recommendRoute()` - O(V log V)

## 💡 Usage Examples

### Loading Data
1. Launch the application with `mvn clean javafx:run`
2. Click "Load Data"
3. Select your `.csv` or `.txt` file

### Finding Routes
1. Click the "Path" button
2. Enter start and end towns (autocomplete available)
3. View top 3 routes highlighted in different colors
4. Review cost breakdown in the sidebar

### Searching
- Use the search bar to find and highlight specific towns
- Click on nodes to view details

## 👥 Authors

Stanley and Linos - 2026

## 📝 License

Ghana Road Transport Network Challenge 2026

---

**Need Help?** Check the full documentation or generate JavaDocs for detailed API information.
