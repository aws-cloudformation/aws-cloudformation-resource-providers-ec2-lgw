# AWS::EC2::LocalGatewayRoute

Describes a route for a local gateway route table.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::EC2::LocalGatewayRoute",
    "Properties" : {
        "<a href="#destinationcidrblock" title="DestinationCidrBlock">DestinationCidrBlock</a>" : <i>String</i>,
        "<a href="#localgatewayroutetableid" title="LocalGatewayRouteTableId">LocalGatewayRouteTableId</a>" : <i>String</i>,
        "<a href="#localgatewayvirtualinterfacegroupid" title="LocalGatewayVirtualInterfaceGroupId">LocalGatewayVirtualInterfaceGroupId</a>" : <i>String</i>,
        "<a href="#networkinterfaceid" title="NetworkInterfaceId">NetworkInterfaceId</a>" : <i>String</i>,
    }
}
</pre>

### YAML

<pre>
Type: AWS::EC2::LocalGatewayRoute
Properties:
    <a href="#destinationcidrblock" title="DestinationCidrBlock">DestinationCidrBlock</a>: <i>String</i>
    <a href="#localgatewayroutetableid" title="LocalGatewayRouteTableId">LocalGatewayRouteTableId</a>: <i>String</i>
    <a href="#localgatewayvirtualinterfacegroupid" title="LocalGatewayVirtualInterfaceGroupId">LocalGatewayVirtualInterfaceGroupId</a>: <i>String</i>
    <a href="#networkinterfaceid" title="NetworkInterfaceId">NetworkInterfaceId</a>: <i>String</i>
</pre>

## Properties

#### DestinationCidrBlock

The CIDR block used for destination matches.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### LocalGatewayRouteTableId

The ID of the local gateway route table.

_Required_: No

_Type_: String

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### LocalGatewayVirtualInterfaceGroupId

The ID of the virtual interface group.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NetworkInterfaceId

The ID of the network interface.

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### State

The state of the route.

#### Type

The route type.

