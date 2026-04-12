import Foundation

/// Ключи совпадают с `strings.xml` на Android (`values` / `values-ru`).
enum L10n {
    static func tr(_ key: String, _ args: CVarArg...) -> String {
        let format = NSLocalizedString(key, tableName: nil, bundle: .main, value: key, comment: "")
        if args.isEmpty { return format }
        return String(format: format, locale: Locale.current, arguments: args)
    }

    /// Как `plurals/blocks_count` на Android (RU: one / few / many).
    static func blocksCount(_ n: Int) -> String {
        let code = Locale.current.language.languageCode?.identifier ?? "en"
        if code == "ru" {
            let mod10 = n % 10
            let mod100 = n % 100
            if mod100 >= 11, mod100 <= 14 { return tr("blocks_many_ru", n) }
            if mod10 == 1 { return tr("blocks_one_ru", n) }
            if mod10 >= 2, mod10 <= 4 { return tr("blocks_few_ru", n) }
            return tr("blocks_many_ru", n)
        }
        return n == 1 ? tr("blocks_one_en", n) : tr("blocks_other_en", n)
    }
}
