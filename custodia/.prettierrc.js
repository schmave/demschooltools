export default {
  printWidth: 100,
  tabWidth: 2,
  trailingComma: "all",
  singleQuote: false,
  semi: true,
  plugins: [import.meta.resolve("@trivago/prettier-plugin-sort-imports")],
  importOrder: ["^[./]"],
  importOrderSeparation: true,
  importOrderSortSpecifiers: true,
};
