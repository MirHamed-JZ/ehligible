import SwiftUI
import UIKit
import WidgetKit

// MARK: - Timeline

struct CountdownEntry: TimelineEntry {
    let date: Date
    /// The numbers for this day; nil before onboarding or once the precomputed days have run out.
    let day: WidgetSnapshot.Day?
    let target: WidgetSnapshot.Target
    let reviewDays: Int
    /// False before onboarding, when the widget invites the person to open the app.
    let hasSnapshot: Bool
    var style: WidgetSnapshot.Style = .system

    static var sample: CountdownEntry {
        CountdownEntry(
            date: Date(),
            day: WidgetSnapshot.Day(
                date: WidgetSnapshot.key(for: Date()),
                daysLeft: 429,
                countdownDate: nil,
                creditedDays: 667,
                progress: 0.61,
                celebrate: false,
                status: .inCanada,
                country: nil
            ),
            target: .exact,
            reviewDays: 0,
            hasSnapshot: true
        )
    }
}

struct CountdownProvider: TimelineProvider {
    func placeholder(in context: Context) -> CountdownEntry {
        .sample
    }

    func getSnapshot(in context: Context, completion: @escaping (CountdownEntry) -> Void) {
        completion(context.isPreview ? .sample : timeline(now: Date()).entries.first ?? .sample)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CountdownEntry>) -> Void) {
        completion(timeline(now: Date()))
    }

    private func timeline(now: Date) -> Timeline<CountdownEntry> {
        let snapshot = WidgetSnapshot.load()
        let schedule: [(date: Date, day: WidgetSnapshot.Day?)] = snapshot?.schedule(from: now) ?? [(date: now, day: nil)]
        let entries = schedule.map { item in
            CountdownEntry(
                date: item.date,
                day: item.day,
                target: snapshot?.target ?? .exact,
                reviewDays: snapshot?.reviewDays ?? 0,
                hasSnapshot: snapshot != nil,
                style: snapshot?.style ?? .system
            )
        }
        // The app reloads the widget whenever its numbers change; this only covers a snapshot that ran out.
        let policy: TimelineReloadPolicy = entries.count > 1 ? .atEnd : .after(now.addingTimeInterval(6 * 3600))
        return Timeline(entries: entries, policy: policy)
    }
}

// MARK: - Views

struct CountdownWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: CountdownEntry

    var body: some View {
        switch family {
        case .accessoryCircular:
            CircularView(entry: entry)
        case .accessoryRectangular:
            RectangularView(entry: entry)
        case .accessoryInline:
            Text(verbatim: Texts.inline(entry))
        case .systemMedium:
            MediumView(entry: entry)
                .widgetStyle(entry.style)
        default:
            SmallView(entry: entry)
                .widgetStyle(entry.style)
        }
    }
}

struct SmallView: View {
    let entry: CountdownEntry

    var body: some View {
        if let day = entry.day {
            VStack(spacing: 2) {
                LeafProgress(progress: day.progress)
                    .frame(width: 70, height: 70)
                Headline(day: day, numberSize: 30, alignment: .center)
            }
        } else {
            Message(text: Texts.message(entry))
        }
    }
}

struct MediumView: View {
    @Environment(\.palette) private var palette
    let entry: CountdownEntry

    var body: some View {
        if let day = entry.day {
            HStack(spacing: 14) {
                LeafProgress(progress: day.progress)
                    .frame(width: 104, height: 104)
                VStack(alignment: .leading, spacing: 3) {
                    Headline(day: day, numberSize: 34, alignment: .leading)
                    if let line = Texts.dateLine(day, target: entry.target) {
                        // French dates are long ("Demande possible dès le 2 janv. 2028"): shrink, don't wrap.
                        Text(verbatim: line)
                            .font(.caption)
                            .foregroundStyle(palette.ink)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    Text(verbatim: Texts.status(day))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Text(verbatim: Texts.counted(day))
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    if entry.reviewDays > 0 {
                        Text(verbatim: "⚠️ " + Texts.review(entry.reviewDays))
                            .font(.caption2)
                            .foregroundStyle(palette.mapleText)
                    }
                }
                Spacer(minLength: 0)
            }
        } else {
            Message(text: Texts.message(entry))
        }
    }
}

private struct Headline: View {
    @Environment(\.palette) private var palette
    let day: WidgetSnapshot.Day
    let numberSize: CGFloat
    let alignment: HorizontalAlignment

    var body: some View {
        VStack(alignment: alignment, spacing: 0) {
            if day.celebrate {
                Text(verbatim: "🎉")
                    .font(.system(size: numberSize))
                Text(verbatim: Texts.eligible)
                    .font(.caption.bold())
                    .foregroundStyle(palette.ink)
                    .multilineTextAlignment(alignment == .leading ? .leading : .center)
            } else if let left = day.daysLeft {
                Text(verbatim: Texts.number(left))
                    .font(.system(size: numberSize, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.mapleText)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(verbatim: Texts.daysToGo(left))
                    .font(.caption)
                    .foregroundStyle(palette.ink)
            }
        }
    }
}

private struct Message: View {
    @Environment(\.palette) private var palette
    let text: String

    var body: some View {
        VStack(spacing: 6) {
            LeafProgress(progress: 0)
                .frame(width: 44, height: 44)
            Text(verbatim: text)
                .font(.caption)
                .foregroundStyle(palette.ink)
                .multilineTextAlignment(.center)
        }
    }
}

struct CircularView: View {
    let entry: CountdownEntry

    var body: some View {
        if let day = entry.day, !day.celebrate, let left = day.daysLeft {
            Gauge(value: min(max(day.progress, 0), 1)) {
                Image(systemName: "leaf.fill")
            } currentValueLabel: {
                Text(verbatim: Texts.number(left))
            }
            .gaugeStyle(.accessoryCircular)
        } else {
            ZStack {
                AccessoryWidgetBackground()
                if entry.day?.celebrate == true {
                    Text(verbatim: "🎉")
                } else {
                    Image(systemName: "leaf.fill")
                }
            }
        }
    }
}

struct RectangularView: View {
    let entry: CountdownEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            if let day = entry.day {
                if day.celebrate {
                    Text(verbatim: "🎉 " + Texts.eligible)
                        .font(.headline)
                        .widgetAccentable()
                } else if let left = day.daysLeft {
                    Text(verbatim: Texts.number(left) + " " + Texts.daysToGo(left))
                        .font(.headline)
                        .widgetAccentable()
                    if let line = Texts.dateLine(day, target: entry.target) {
                        Text(verbatim: line)
                            .font(.caption)
                            .lineLimit(1)
                            .minimumScaleFactor(0.7)
                    }
                }
                ProgressView(value: min(max(day.progress, 0), 1))
            } else {
                Text(verbatim: Texts.message(entry))
                    .font(.caption)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Leaf

/// The Eh-ligible leaf filling up inside its ring, drawn like the app and the Android widget.
struct LeafProgress: View {
    @Environment(\.palette) private var palette
    let progress: Double

    var body: some View {
        GeometryReader { proxy in
            let side = min(proxy.size.width, proxy.size.height)
            let ringWidth = side * 0.06
            let radius = side / 2 - ringWidth
            let leafSide = radius * 1.45
            let fraction = min(max(progress, 0), 1)
            ZStack {
                Circle()
                    .stroke(palette.track, lineWidth: ringWidth)
                    .frame(width: radius * 2, height: radius * 2)
                if fraction > 0 {
                    Circle()
                        .trim(from: 0, to: fraction)
                        .stroke(
                            fraction >= 1 ? palette.gold : palette.maple,
                            style: StrokeStyle(lineWidth: ringWidth, lineCap: .round)
                        )
                        .rotationEffect(.degrees(-90))
                        .frame(width: radius * 2, height: radius * 2)
                }
                ZStack {
                    RoundedLeaf(color: palette.leafEmpty, joinWidth: side * 0.03)
                    if fraction > 0 {
                        // The water line rises with the leaf, tilted with it (as on Android).
                        RoundedLeaf(color: palette.maple, joinWidth: side * 0.03)
                            .mask(alignment: .bottom) {
                                Rectangle().frame(height: leafSide * (fraction * 0.97 + 0.01))
                            }
                    }
                }
                .frame(width: leafSide, height: leafSide)
                .offset(y: -leafSide * 0.02)
                .rotationEffect(.degrees(-12))
            }
            .frame(width: side, height: side)
            .position(x: proxy.size.width / 2, y: proxy.size.height / 2)
        }
    }
}

private struct RoundedLeaf: View {
    let color: Color
    let joinWidth: CGFloat

    var body: some View {
        LeafShape()
            .fill(color)
            .overlay(LeafShape().stroke(color, style: StrokeStyle(lineWidth: joinWidth, lineJoin: .round)))
    }
}

/// The 11-point maple leaf in a 100×100 box (the same points as Kotlin's `LeafShape`).
struct LeafShape: Shape {
    private static let points: [(CGFloat, CGFloat)] = [
        (50, 4), (57, 18), (65, 14), (62, 36), (80, 22), (84, 32), (96, 30), (88, 48), (94, 54), (70, 70), (73, 80),
        (53, 77), (53, 97), (47, 97), (47, 77), (27, 80), (30, 70), (6, 54), (12, 48), (4, 30), (16, 32), (20, 22),
        (38, 36), (35, 14), (43, 18),
    ]

    func path(in rect: CGRect) -> Path {
        var path = Path()
        for (index, point) in Self.points.enumerated() {
            let location = CGPoint(
                x: rect.minX + point.0 * rect.width / 100,
                y: rect.minY + point.1 * rect.height / 100
            )
            if index == 0 {
                path.move(to: location)
            } else {
                path.addLine(to: location)
            }
        }
        path.closeSubpath()
        return path
    }
}

// MARK: - Style and text

/// The widget's colours for one style (Kotlin's `WidgetColors`: keep the values in step).
struct Palette {
    /// The filled leaf and the ring.
    let maple: Color
    /// The ring once all the days are in.
    let gold: Color
    /// The big number.
    let mapleText: Color
    let leafEmpty: Color
    let track: Color
    let background: Color
    let ink: Color

    /// Light or dark like the phone.
    static let system = Palette(
        maple: fixed(0xD62828),
        gold: fixed(0xF4A259),
        mapleText: dynamic(light: 0xD62828, dark: 0xFF8A80),
        leafEmpty: dynamic(light: 0xF6DCD8, dark: 0x3D2A2B),
        track: dynamic(light: 0xF5DDDA, dark: 0x534341),
        background: dynamic(light: 0xFFF8F5, dark: 0x1A1112),
        ink: dynamic(light: 0x231917, dark: 0xF1DEDC)
    )

    static let light = Palette(
        maple: fixed(0xD62828),
        gold: fixed(0xF4A259),
        mapleText: fixed(0xD62828),
        leafEmpty: fixed(0xF6DCD8),
        track: fixed(0xF5DDDA),
        background: fixed(0xFFF8F5),
        ink: fixed(0x231917)
    )

    static let dark = Palette(
        maple: fixed(0xD62828),
        gold: fixed(0xF4A259),
        mapleText: fixed(0xFF8A80),
        leafEmpty: fixed(0x3D2A2B),
        track: fixed(0x534341),
        background: fixed(0x1A1112),
        ink: fixed(0xF1DEDC)
    )

    /// Red with a white leaf, like the flag.
    static let mapleRed = Palette(
        maple: fixed(0xFFFFFF),
        gold: fixed(0xF4A259),
        mapleText: fixed(0xFFFFFF),
        leafEmpty: fixed(0xC8474B),
        track: fixed(0xC23B40),
        background: fixed(0xA4161A),
        ink: fixed(0xFFFFFF)
    )

    static func of(_ style: WidgetSnapshot.Style) -> Palette {
        switch style {
        case .system: return .system
        case .light: return .light
        case .dark: return .dark
        case .maple: return .mapleRed
        }
    }

    /// The light or dark look that a fixed style's system colours (such as `.secondary` text) take;
    /// nil to follow the phone.
    static func colorScheme(for style: WidgetSnapshot.Style) -> ColorScheme? {
        switch style {
        case .system: return nil
        case .light: return .light
        case .dark, .maple: return .dark
        }
    }

    private static func fixed(_ rgb: Int) -> Color {
        Color(uiColor(rgb))
    }

    private static func dynamic(light: Int, dark: Int) -> Color {
        Color(UIColor { traits in uiColor(traits.userInterfaceStyle == .dark ? dark : light) })
    }

    private static func uiColor(_ rgb: Int) -> UIColor {
        UIColor(
            red: CGFloat((rgb >> 16) & 0xFF) / 255,
            green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255,
            alpha: 1
        )
    }
}

private struct PaletteKey: EnvironmentKey {
    static let defaultValue = Palette.system
}

extension EnvironmentValues {
    var palette: Palette {
        get { self[PaletteKey.self] }
        set { self[PaletteKey.self] = newValue }
    }
}

private struct WidgetStyleModifier: ViewModifier {
    let style: WidgetSnapshot.Style

    @ViewBuilder
    func body(content: Content) -> some View {
        if let scheme = Palette.colorScheme(for: style) {
            content
                .environment(\.palette, Palette.of(style))
                .environment(\.colorScheme, scheme)
        } else {
            content.environment(\.palette, Palette.of(style))
        }
    }
}

extension View {
    /// Draws a home-screen widget in the colours chosen in the app.
    func widgetStyle(_ style: WidgetSnapshot.Style) -> some View {
        modifier(WidgetStyleModifier(style: style))
    }
}

/// Finds the bundle this code was built into: the widget extension, or the tests.
private final class BundleToken {}

/// The widget's wording, matching the app's (Localizable.strings and .stringsdict, English and French).
enum Texts {
    static let requiredDays = 1095

    /// Set only by tests, to render each language; the widget follows the system.
    private static var languageBundle: Bundle?
    private static var localeOverride: Locale?

    private static var bundle: Bundle {
        languageBundle ?? Bundle(for: BundleToken.self)
    }

    static var locale: Locale {
        localeOverride ?? .current
    }

    /// Renders in `language` ("en", "fr"), or back in the system's language with nil.
    static func use(language: String?) {
        let base = Bundle(for: BundleToken.self)
        guard let language, let path = base.path(forResource: language, ofType: "lproj") else {
            languageBundle = nil
            localeOverride = nil
            return
        }
        languageBundle = Bundle(path: path)
        localeOverride = Locale(identifier: language + "_CA")
    }

    static func localized(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    static var eligible: String {
        localized("eligible")
    }

    static func number(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.locale = locale
        return formatter.string(from: NSNumber(value: value)) ?? String(value)
    }

    static func daysToGo(_ count: Int) -> String {
        String(format: localized("days_to_go"), locale: locale, count)
    }

    static func review(_ count: Int) -> String {
        String(format: localized("review_days"), locale: locale, count)
    }

    static func dateLine(_ day: WidgetSnapshot.Day, target: WidgetSnapshot.Target) -> String? {
        guard !day.celebrate, let key = day.countdownDate, let date = WidgetSnapshot.startOfDay(key) else {
            return nil
        }
        let formatted = date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted, locale: locale))
        return String(format: localized(target == .safer ? "safer_date" : "apply_from"), formatted)
    }

    static func counted(_ day: WidgetSnapshot.Day) -> String {
        String(format: localized("counted"), number(min(day.creditedDays, requiredDays)), number(requiredDays))
    }

    static func status(_ day: WidgetSnapshot.Day) -> String {
        switch day.status {
        case .inCanada:
            return "🇨🇦 " + localized("status.in_canada")
        case .abroad:
            guard let code = day.country else { return "✈️ " + localized("status.abroad_unknown") }
            let name = locale.localizedString(forRegionCode: code) ?? code
            return flag(code) + " " + String(format: localized("status.abroad"), name)
        case .nearBorder:
            return "🧭 " + localized("status.near_border")
        case .unknown:
            return "📍 " + localized("status.unknown")
        }
    }

    static func message(_ entry: CountdownEntry) -> String {
        localized(entry.hasSnapshot ? "stale" : "setup")
    }

    static func inline(_ entry: CountdownEntry) -> String {
        guard let day = entry.day else { return "🍁 Eh-ligible" }
        if day.celebrate { return "🎉 " + eligible }
        guard let left = day.daysLeft else { return "🍁 Eh-ligible" }
        return "🍁 " + number(left) + " " + daysToGo(left)
    }

    /// The flag emoji for an ISO country code, made of regional indicator symbols.
    private static func flag(_ code: String) -> String {
        var flag = ""
        for scalar in code.uppercased().unicodeScalars {
            guard let symbol = UnicodeScalar(127_397 + scalar.value) else { return "✈️" }
            flag.unicodeScalars.append(symbol)
        }
        return flag
    }
}
