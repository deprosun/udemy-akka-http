import Foundation

struct Concert: Decodable, Identifiable {
    let id        = UUID()
    let name:     String
    let date:     String
    let time:     String
    let venue:    String
    let url:      String
    let imageUrl: String

    enum CodingKeys: String, CodingKey {
        case name, date, time, venue, url, imageUrl
    }
}
