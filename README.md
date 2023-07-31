The basic code template developed in kotlin. You can use this project as a starting point of your own app.

- Kotlin, MVVM, Dependency Injection(Dagger), Network calling(Retrofit)
- Custom progress dialog, Common Utils, Base classes, Preference Utils

-------------------------------------------------------

1. Four activities are used in entire code, i). SplashActivity ii). MainActivity iii). ChooseLocationActivity iv). NewSearchLocationActivity.
2. MainActivity is a Map screen which will initially show the current location(LatLong/ Marker) and button to select Home location. After selecting home location it will show the route between current and home location(LatLong/Distance/Markers/Polyline).
3. ChooseLocationActivity is a Map/Location listing screen from where you can pick/search the location.
4. NewSearchLocationActivity is an additional search place screen.
5. Used Google Map, Google direction api to show the route(Navigation) and Places api.
6. Displayed distance from the direction api response.