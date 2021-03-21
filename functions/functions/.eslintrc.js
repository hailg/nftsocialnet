module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  extends: ["eslint:recommended", "google"],
  plugins: ["prettier"],
  rules: {
    "prettier/prettier": "error",
  },
  parser: "babel-eslint",
  extends: ["plugin:prettier/recommended"],
};
