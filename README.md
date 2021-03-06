# Android Cell Service


### What it does
CellActivity collects metadata about mobile data traffic, calls and text messages with respect to the cell towers connected to. The goal is to emulate [Call Detail Records](https://en.wikipedia.org/wiki/Call_detail_record)(CDR) and Data Detail Records(DDR) as closely as possible, but gathered from a mobile device.

### Why use it
Every record you create with your mobile device has a coarse location associated to it. A mobile network provider has knowledge of at least the same amount and quality of information to deduce a movement profile and potentially places you have been to throughout the day. This is an important component of [Telecommunications Data Retention](https://en.wikipedia.org/wiki/Telecommunications_data_retention).

CellActivity helps visualizing and understanding the accuracy of this information. It will allow researchers to work with CDRs and DDRs and could also be used as a foundation for energy efficient movement-activity tracking.

### What it cannot collect

* Periodic [Location Updates](https://en.wikipedia.org/wiki/Mobility_management#Location_update_procedure) as defined by a Mobile Network Operator. I.e. every 6 hours or 30 minutes.

### Applications using this library
* [MobileFootprint](https://github.com/MobileFootprint/MobileFootprint) - an app that visualizes the collected data in aggregated as well as interactive form.

### License
The source code of this project is licensed under GPL v3. Please review [License file](LICENSE) for more details.
