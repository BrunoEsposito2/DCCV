import React from "react";
import { shallow } from "enzyme";
import Videofeed from "./videofeed";

describe("Videofeed", () => {
  test("matches snapshot", () => {
    const wrapper = shallow(<Videofeed />);
    expect(wrapper).toMatchSnapshot();
  });
});
